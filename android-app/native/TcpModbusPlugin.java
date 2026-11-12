package it.sunengo.datihuawei;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.SocketFactory;

/**
 * Funzioni native dell'app Dati Huawei.
 * - open / transact / close: connessione TCP (Modbus TCP) verso l'inverter. La pagina web costruisce e legge
 *   le trame Modbus; qui si inviano i byte e si riceve una risposta completa. Usata solo in lettura.
 * - share: salva un file (json, csv) e apre la condivisione di Android.
 * - print: stampa un documento HTML con la stampa di sistema (anche "Salva come PDF").
 */
@CapacitorPlugin(name = "TcpModbus")
public class TcpModbusPlugin extends Plugin {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Socket socket;
    private WebView printView;

    /** Il Wi-Fi dell'inverter non ha Internet: Android userebbe i dati mobili, quindi il socket va legato al Wi-Fi. */
    private SocketFactory wifiSocketFactory() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities c = cm.getNetworkCapabilities(n);
                if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return n.getSocketFactory();
                }
            }
        }
        return SocketFactory.getDefault();
    }

    private void closeQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
        socket = null;
    }

    @PluginMethod
    public void open(final PluginCall call) {
        final String host = call.getString("host", "");
        final int port = call.getInt("port", 502);
        final int timeout = call.getInt("timeout", 5000);
        io.execute(() -> {
            try {
                closeQuietly();
                Socket s = wifiSocketFactory().createSocket();
                s.connect(new InetSocketAddress(host, port), timeout);
                s.setSoTimeout(timeout);
                s.setTcpNoDelay(true);
                socket = s;
                JSObject r = new JSObject();
                r.put("ok", true);
                call.resolve(r);
            } catch (Exception e) {
                closeQuietly();
                call.reject("Connessione non riuscita: " + e.getMessage(), e);
            }
        });
    }

    /** Invia una trama Modbus TCP e restituisce la risposta completa (intestazione MBAP + dati), in base64. */
    @PluginMethod
    public void transact(final PluginCall call) {
        final String data = call.getString("data", "");
        final int timeout = call.getInt("timeout", 5000);
        io.execute(() -> {
            try {
                if (socket == null || socket.isClosed()) throw new IllegalStateException("non collegato");
                socket.setSoTimeout(timeout);
                byte[] req = Base64.decode(data, Base64.NO_WRAP);
                OutputStream out = socket.getOutputStream();
                out.write(req);
                out.flush();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                byte[] head = new byte[6];
                in.readFully(head);
                int len = ((head[4] & 0xff) << 8) | (head[5] & 0xff);
                if (len < 2 || len > 260) throw new IllegalStateException("risposta non valida");
                byte[] all = new byte[6 + len];
                System.arraycopy(head, 0, all, 0, 6);
                in.readFully(all, 6, len);
                JSObject r = new JSObject();
                r.put("data", Base64.encodeToString(all, Base64.NO_WRAP));
                call.resolve(r);
            } catch (Exception e) {
                closeQuietly();
                call.reject("Lettura non riuscita: " + e.getMessage(), e);
            }
        });
    }

    @PluginMethod
    public void close(final PluginCall call) {
        io.execute(() -> {
            closeQuietly();
            call.resolve();
        });
    }

    @PluginMethod
    public void share(final PluginCall call) {
        final String name = call.getString("name", "file.txt");
        final String mime = call.getString("mime", "application/octet-stream");
        final String data = call.getString("data", "");
        io.execute(() -> {
            try {
                File dir = new File(getContext().getCacheDir(), "export");
                if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("cartella non disponibile");
                File f = new File(dir, name.replaceAll("[^\\w.\\-]", "_"));
                try (FileOutputStream o = new FileOutputStream(f)) {
                    o.write(Base64.decode(data, Base64.NO_WRAP));
                }
                Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(mime);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                final Intent chooser = Intent.createChooser(send, name);
                getActivity().runOnUiThread(() -> {
                    try {
                        getActivity().startActivity(chooser);
                        call.resolve();
                    } catch (Exception e) {
                        call.reject("Condivisione non riuscita: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                call.reject("Salvataggio non riuscito: " + e.getMessage(), e);
            }
        });
    }

    @PluginMethod
    public void print(final PluginCall call) {
        final String html = call.getString("html", "");
        final String name = call.getString("name", "Dati Huawei");
        getActivity().runOnUiThread(() -> {
            try {
                final WebView wv = new WebView(getActivity());
                wv.setWebViewClient(new WebViewClient() {
                    private boolean done = false;

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        if (done) return;
                        done = true;
                        PrintManager pm = (PrintManager) getActivity().getSystemService(Context.PRINT_SERVICE);
                        PrintAttributes attrs = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .build();
                        pm.print(name, view.createPrintDocumentAdapter(name), attrs);
                        call.resolve();
                    }
                });
                wv.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
                printView = wv; // riferimento tenuto finché la stampa è in corso
            } catch (Exception e) {
                call.reject("Stampa non riuscita: " + e.getMessage(), e);
            }
        });
    }
}
