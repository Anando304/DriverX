package ca.sajal.g2_g_app;

import android.content.Intent;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static java.sql.DriverManager.println;


public class firebasedata extends AppCompatActivity {
    public ListView mListView;
    public EditText search;
    public Button searchbutton;
    public String selectedItem = "";
    public String selectedRoute = "";
    String searchvalue = "";
    public ArrayList<String> gpsroutes = new ArrayList<>(); //list/arraylist to hold new routes of the cities.
    public ArrayList<String> gpsrouteschilds = new ArrayList<>(); //list/arraylist to hold new routes of the cities.
    public List<String> coordinates = new ArrayList<String>();
    public DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("cities"); //links myRef variable to the database


    public void search(){
        search = (EditText) findViewById(R.id.search);
        searchvalue = search.getText().toString().toLowerCase().trim();
        if(gpsroutes.indexOf(searchvalue)!=-1){ //If search element does not exist, then it'll output -1. So checks if search element exists.
            gpsroutes.clear(); // clear all previous elements so doesn't show unwanted elements up in UI after search.
            gpsroutes.add(searchvalue); //add element to the cleared array to be displayed to screen.
        }

        else if(searchvalue.equals("")){
        }

        else{
            gpsroutes.clear(); // clear all elements so doesn't show up in UI after search.
            gpsroutes.add(searchvalue + " does not exist");
        }


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebasedata);

        searchbutton = (Button) findViewById(R.id.searchbutton);
       /* checkDatabase(new firebaseCallback() {
            @Override
            public void onCallback(List<String> gpsroutes) {
                DO CODE IN HERE
                mListView = (ListView) findViewById(R.id.listview);
                ArrayAdapter <String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, gpsroutes);
                mListView.setAdapter(arrayAdapter);
  }

   }); */

        routesChildDatabase(new routesCallback() { //calls the method back once loaded the data from database
            @Override
            public void onCallback(List<String> gpsrouteschilds) {
                gpsroutes.clear();
                mListView = (ListView) findViewById(R.id.listview); //Listview is used as a UI element to hold arrays.
                ArrayAdapter <String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, gpsrouteschilds);
                mListView.setAdapter(arrayAdapter); //An adapter manages list data and adapts it to the rows of the list view for dynamic lists from databases



                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() { //check if list item is clicked
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                                            long id) {

                        selectedItem = (String) parent.getItemAtPosition(position); //Finds out which button is pressed

                        routesChildDatabase(new routesCallback() { //calls the method back once loaded the data from database

                            @Override
                            public void onCallback(List<String> gpsrouteschilds) { //Gathers routes of cities and overwrites the previous cities listview.
                                mListView = (ListView) findViewById(R.id.listview);
                                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, gpsrouteschilds);
                                mListView.setAdapter(arrayAdapter); //An adapter manages your data and adapts it to the rows of the list view for dynamic lists from databases


                                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() { //checks which route selected
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        selectedRoute = (String) parent.getItemAtPosition(position); //saves the route pressed as a string
                                            if(!selectedItem.equals("toronto")) { //toronto maps still in progress, so will not intent.
                                                Intent route = new Intent(getApplicationContext(), MainActivity.class);
                                                //If a route is pressed, class will be intented to MainActivity where the GPS/Mapping process occurs

                                                route.putExtra("routeCallback", selectedRoute); //Saves and Intents the selected route to MainActivity
                                                route.putExtra("cityCallback", selectedItem); //Saves and Intents selected city to MainActivity
                                                startActivity(route);
                                                Toast.makeText(getApplicationContext(), selectedRoute, Toast.LENGTH_SHORT).show();

                                            }


                                    }
                                });

                            }

                        });


                    }


                });
                gpsroutes.clear();
            }

        });


        searchbutton.setOnClickListener(new View.OnClickListener() {//Search button listener
            @Override
            public void onClick(View v) {
                checkDatabase(new firebaseCallback() { //finally, call the checkDatabase method once more and pass the an instance of firebaseCallBack but this time
                    // load the final string lists to be associated to the recalled Views and adapters to be up to date with the new updated changes from database info.
                    // Other method/way to get around asynchronous delay function skip glitch is to incorporate all code that requires database inside the onDataChange() method
                    // so that it all runs once the data is successfully loaded from database.
                    @Override
                    public void onCallback(final List<String> gpsroutes) {
                        /*DO CODE IN HERE*/
                        mListView = (ListView) findViewById(R.id.listview);
                        search(); //Runs the functions for the search

                        ArrayAdapter <String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, gpsroutes);
                        mListView.setAdapter(arrayAdapter); //Displays the search to screen



                    }
                });


            }
        });






    }



    private void checkDatabase(final firebaseCallback firebaseCallback){ //method used to retrieve the data from the database and pass myCallBack instance as argument

        myRef.addListenerForSingleValueEvent(new ValueEventListener() { //reads the children/subsections of the list

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot childSnapShot : dataSnapshot.getChildren()) { //does a for loop to loop through each child for the number of children there are
                    String name = childSnapShot.getKey().toLowerCase(); //saves the children as a string

                    Log.d("DEBUGA", name);

                    gpsroutes.add(name); //adds the string of said child to the list. In this case, adds them to the GPS routes.


                }
                firebaseCallback.onCallback(gpsroutes); //runs the interface to retrieve the data once loaded
                Log.d("io.githib.anando304", "crash check");
            }


            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void routesChildDatabase(final routesCallback routesCallback){ //method used to retrieve the data from the database and pass myCallBack instance as argument

        myRef.child(selectedItem).addListenerForSingleValueEvent(new ValueEventListener() { //reads the children/subsections of the list

            @Override
            public void onDataChange(DataSnapshot dataSnapshotnew) {

                for(DataSnapshot childrouteSnapShot : dataSnapshotnew.getChildren()) { //does a for loop to loop through each child for the number of children there are
                    String name1 = childrouteSnapShot.getKey().toLowerCase(); //saves the children as a string

                    Log.d("DEBUGA_NEW", name1);

                    gpsrouteschilds.add(name1); //adds the string of said child to the list. In this case, adds them to the GPS routes.

                }
                routesCallback.onCallback(gpsrouteschilds); //runs the interface to retrieve the data once loaded

            }


            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public interface firebaseCallback{ // Asynchronous API's such as firebase cannot return something that has not loaded even if fraction of a millisecond delay.
        // This interface is similar to a function except it only contains the signature (name, parameters and exceptions). The method is used to wait for firebase to send back data to you once loaded, aka "callback".
        void onCallback(List<String> gpsroutes);
    }

    public interface routesCallback{ // Asynchronous API's such as firebase cannot return something that has not loaded even if fraction of a millisecond delay.
        // This interface is similar to a function except it only contains the signature (name, parameters and exceptions). The method is used to wait for firebase to send back data to you once loaded, aka "callback".
        void onCallback(List<String> gpsrouteschilds);
    }

}

