package val.uteq.examenf;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TableLayout;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Set;

import val.uteq.examenf.utiles.Methods;
import val.uteq.examenf.utiles.MyLogs;
import val.uteq.examenf.utiles.TableModel;

public class MapViewLayout extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view_layout);


        loadMap();
    }

    ArrayList<String[]> lista;

    private void loadMap(){
        //obtener datos heredado del activity principal
        Bundle bundle = this.getIntent().getExtras();
        String data = bundle.getString("countryData");
        JsonObject countryData = Methods.stringToJSON(data);
        MyLogs.info("Mapa View");
        MyLogs.info(data);

        //Preparar tabla
        lista = new ArrayList<>();
        setDataInTable(countryData, true);

        //preparar mapa

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mymap);

        if (mapFragment != null) {
            MapManager mapManager = new MapManager(MapViewLayout.this, countryData);
            mapFragment.getMapAsync(mapManager);

        } else {
            MyLogs.error("Esta nulo el Mapa");
        }
    }

    /*******************************************************************************************
     *                             ADAPTACION DE LOS RESULTADOS                                *
     *******************************************************************************************/
    private void tableAdapt(ArrayList<String[]> lista) {
        //Primero y antes de hacer algo mas, investigar v':


        //obtener la referencia de la tabla en el activity
        TableLayout table = (TableLayout) findViewById(R.id.table);
        //declaramos el objeto que nos creará la tabla dinámica
        TableModel tbModel = new TableModel(MapViewLayout.this, table);
        //indicamos los encabezados de la tabla
        tbModel.setHeaders(new String[]{"Título", "Descripción"});
        //enviamos los datos del cuerpo de la tabla
        tbModel.setRows(lista);
        //configuramos la tabla, colores del encabezado y el cuerpo
        // tanto del texto como el fondo
        tbModel.setHeaderBackGroundColor(R.color.btn_info);
        tbModel.setRowsBackGroundColor(R.color.back_white);

        tbModel.setHeadersForeGroundColor(R.color.back_white);
        tbModel.setRowsForeGroundColor(R.color.back_black);
        //Modifica la tabla a partir de los datos enviados y los parámetros enviados
        tbModel.makeTable();

        MyLogs.info(" FIN ");
    }

    private void setDataInTable(JsonObject obj, Boolean isRoot) {
        Set<String> keys = obj.keySet();
        for (String key : keys) {
            if (!key.equals("CountryInfo")) {
                JsonElement jse = Methods.securGetJSON(obj, key);
                if (!jse.isJsonObject()) {
                    String val = jse.toString();
                    lista.add(new String[]{key, val});
                } else {
                    JsonObject minjso = Methods.JsonElementToJSO(jse);
                    setDataInTable(minjso, false);
                }
            }
        }
        if (isRoot) {
            tableAdapt(lista);
        }
    }
}