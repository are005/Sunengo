package it.sunengo.datihuawei;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Funzioni native dell'app (lettura inverter, condivisione file, stampa)
        registerPlugin(TcpModbusPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
