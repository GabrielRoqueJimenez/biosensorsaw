package com.example.biosensor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.media.session.IMediaControllerCallback;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialInterface;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity
{
    ListView infoapp;
    EditText resultadoglu;
    MenuItem mItem;

    //Se definen las variables para el uso del puerto usb
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    UsbDevice device;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    //Se crea el objeto axis
    private LineGraphSeries<DataPoint> series;
    private GraphView grafica;
    private static int v1, v2, v3, v4;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager)getSystemService(this.USB_SERVICE);
        resultadoglu = findViewById(R.id.resultado);
        grafica = findViewById(R.id.grafica);

        //Se dan acciones a las variables del USB
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        //poner el arreglo de strings en el listview
        String[] pasos = getResources().getStringArray(R.array.texto_info);
        infoapp = findViewById(R.id.Listapasos);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, pasos);
        infoapp.setAdapter(adapter);
    }

    //Se pone en uso la barra de opciones
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_bar, menu);
        mItem = menu.findItem(R.id.sensor);
        return true;
    }
    
    //accionamos los botones de la barra
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            /*case R.id.sensor:
                infoapp.setVisibility(View.INVISIBLE);
                grafica.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Por favor espere, el proceso puede tardar", Toast.LENGTH_SHORT).show();
                return true;*/

            case R.id.rangos:
                startActivity(new Intent(MainActivity.this, Popup_rangos.class));
                return true;

            case R.id.info:
                Toast.makeText(MainActivity.this, "La información estará disponible próximamente", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.exit:
                serialPort.close();
                return true;
        }
        return false;
    }

    //Mandamos soliciud de enlace con el serialport
    public void onClickconectar(MenuItem item)
    {
        infoapp.setVisibility(View.INVISIBLE);
        grafica.setVisibility(View.VISIBLE);
        Toast.makeText(MainActivity.this, "Por favor espere, el proceso puede tardar", Toast.LENGTH_SHORT).show();

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty())
        {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 6790 || deviceVID == 1659)
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device,pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
    }

    //Se conecta con el serialport
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ACTION_USB_PERMISSION))
            {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted)
                {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device,connection);
                    if (serialPort!=null)
                    {
                        if(serialPort.open())
                        {
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallBack);
                            Toast.makeText(context, "Conexión con el puerto serial exitosa", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Log.d("SERIAL","El puerto no se conectó");
                            Toast.makeText(context, "Conexión con el puerto serial fallida", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Log.d("SERIAL","El puerto no se encontó");
                        Toast.makeText(context, "No existe el puerto", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Log.d("SERIAL","El permiso no se otorgó");
                    Toast.makeText(context, "Permiso Denegado", Toast.LENGTH_SHORT).show();
                }
            }
            else if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))
            {
               onClickconectar(mItem);
            }
        };
    };

    UsbSerialInterface.UsbReadCallback mCallBack;
    {
        mCallBack = new UsbSerialInterface.UsbReadCallback() {
            @Override
            public void onReceivedData(byte[] arg0) {
                String data = null;
                try {
                    data = new String(arg0, "UTF-8");
                    data.concat("\n");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}