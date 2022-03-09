package val.uteq.examenf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.android.volley.RequestQueue;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonObject;

import val.uteq.examenf.utiles.Methods;
import val.uteq.examenf.utiles.MyLogs;

public class MapManager implements OnMapReadyCallback, GoogleMap.OnMapClickListener
        //GoogleMap.InfoWindowAdapter,
        //GoogleMap.OnInfoWindowClickListener, AdapterView.OnItemSelectedListener
{

    private GoogleMap myMap = null;
    private Context context;

    private RequestQueue queue;
    private JsonObject ObjectLocation = null;

    public MapManager(Context ctx, JsonObject data) {
        this.context = ctx;
        this.ObjectLocation = data;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap != null) {
            //cuando el mapa esté completamente cargado
            myMap = googleMap;
            // habilitar controles de zoom
            myMap.getUiSettings().setZoomControlsEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(true);
            //esta clase tiene el método para el clic del mapa, por ello se le asigna this
            myMap.setOnMapClickListener(MapManager.this);
            //vista customizada
            //myMap.setOnInfoWindowClickListener(MapManager.this);

            //myMap.setInfoWindowAdapter(MapManager.this);
            myMap.setMapType(4);

            genericPoinst();
        } else {
            MyLogs.error("Esta nulo el Mapa");
            //cierra el modal una vez haya cargado la api
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //clic en el mapa
    }

    public GoogleMap getMyMap() {
        return myMap;
    }

    public void setMyMap(GoogleMap myMap) {
        this.myMap = myMap;
    }


    public void genericPoinst() {

        if (ObjectLocation != null) {
            String countryName = Methods.JsonToString(ObjectLocation, "Name", "None");
            JsonObject jsonCapital = Methods.JsonToSubJSON(ObjectLocation, "Capital");
            double[] points = Methods.JsonToDubleVector(jsonCapital, "GeoPt");
            if (points.length > 0) {
                LatLng location = new LatLng(points[0], points[1]);
                Marker mark = myMap.addMarker(new
                        MarkerOptions().position(location)
                        .title(Methods.JsonToString(jsonCapital, "Name", "Name"))
                        .snippet("Capital de: " + countryName));

                Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.start);
                //rescalar icono
                bm = Bitmap.createScaledBitmap(bm, 100, 120, false);
                //asignar icono
                //mark.setIcon(BitmapDescriptorFactory.fromBitmap(bm));
                CameraPosition camPos = new CameraPosition.Builder()
                        .target(location)
                        .zoom(3)
                        .bearing(0)
                        .build();
                CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
                myMap.animateCamera(camUpd3);
            }


            JsonObject GeoRectangle = Methods.JsonToSubJSON(ObjectLocation, "GeoRectangle");

            double West = Methods.JsonTodouble(GeoRectangle, "West", 0.0);
            double East = Methods.JsonTodouble(GeoRectangle, "East", 0.0);
            double North = Methods.JsonTodouble(GeoRectangle, "North", 0.0);
            double South = Methods.JsonTodouble(GeoRectangle, "South", 0.0);
            MyLogs.info(West + " :: " + East + " :: " + North + " :: " + South);
            PolylineOptions polyPais = new PolylineOptions()
                    .clickable(false)
                    .add(new LatLng(North, West),//0 0
                    new LatLng(North, East),//0 1
                    new LatLng(South, East),// 1 1
                    new LatLng(South, West),//1 0
                    new LatLng(North, West));//0 0
            polyPais.color(Color.RED);
            polyPais.width(5);

            myMap.addPolyline(polyPais);

            JsonObject CountryCodes = Methods.JsonToSubJSON(ObjectLocation, "CountryCodes");
            String flag = Methods.JsonToString(CountryCodes, "iso2", "");
            //MyLogs.info("http://www.geognos.com/api/en/countries/flag/" + flag + ".png");
            /*Picasso.get().load("http://www.geognos.com/api/en/countries/flag/" + flag + ".png")
                    .error(R.drawable.portada)
                    .into(imgflag);*/

            /*****************  PONER LOS RESULTADOS *************/

            //mover camara
            double[] GeoPt = Methods.JsonToDubleVector(ObjectLocation, "GeoPt");

            if (GeoPt.length == 2) {
                LatLng location = new LatLng(GeoPt[0], GeoPt[1]);
                CameraPosition camPos = new CameraPosition.Builder()
                        .target(location)
                        .zoom(3)
                        .bearing(0)
                        .build();
                CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
                myMap.animateCamera(camUpd3);
            }
        }
    }
}
