package com.ozgurzorlu.travelbook

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.ozgurzorlu.travelbook.databinding.ActivityMapsBinding
import com.ozgurzorlu.travelbook.model.Place
import com.ozgurzorlu.travelbook.roomdb.Dao
import com.ozgurzorlu.travelbook.roomdb.PlaceDB
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private lateinit var sharedPreferences: SharedPreferences
    private var trackBool: Boolean? = null

    private var selectedLat : Double? = null
    private var selectedLong : Double? = null

    private lateinit var db : PlaceDB
    private lateinit var placeDao : Dao

    val compositeDisposable = CompositeDisposable()

    var selectedPlace : Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.ozgurzorlu.travelbook", MODE_PRIVATE)
        trackBool = false

        selectedLat = 0.0
        selectedLong = 0.0

        db = Room.databaseBuilder(applicationContext,PlaceDB::class.java,"Places").build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapLongClickListener (this)

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info == "new"){

            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE

            //Casting
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener{
                override fun onLocationChanged(location: Location) {

                    trackBool = sharedPreferences.getBoolean("trackBool",false)
                    if (trackBool==false){
                        val userLocation = LatLng(location.latitude,location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,16f))
                        sharedPreferences.edit().putBoolean("trackBool",true).apply()
                    }

                }

            }
            // izin alindi mi diye kontrol ediliyor:
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //mesaj gösterilecek mi diye kontrol:
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.root,"Permission needed for location!",Snackbar.LENGTH_INDEFINITE).setAction("Give permission"){
                        //request permission
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                }
                else{
                    // request permiision
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            // permission granted
            else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)

                val lastLocation =locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null){
                    val userLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,16f))
                }
                mMap.isMyLocationEnabled = true
            }
        }
        else{
            mMap.clear()
            selectedPlace = intent.getSerializableExtra("selectedPlace") as? Place // place cinsinden old. icin as kullaniliyor.

            selectedPlace?.let { // Eger null degilse islemler yapilacak.
                val latLong = LatLng(it.latitude,it.longitude)

                mMap.addMarker(MarkerOptions().position(latLong).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong,16f))

                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
                binding.placeText.setText(it.name)

            }

        }

       /*
        val merkezCami = LatLng(36.99163278844503, 35.334219795900765)
        mMap.addMarker(MarkerOptions().position(merkezCami).title("Merkez Cami"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(merkezCami,16f))

        */
    }
    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    // permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)

                    val lastLocation =locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null){
                        val userLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,16f))
                    }
                    mMap.isMyLocationEnabled = true
                }

                else{
                    // permission denied
                    Toast.makeText(this@MapsActivity,"Permission needed!",Toast.LENGTH_LONG).show()
                }
            }
    }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))

        selectedLat = p0.latitude
        selectedLong = p0.longitude

        binding.saveButton.isEnabled = true

    }
    fun save(view:View){

        if (selectedLat != null && selectedLong != null){
            val place = Place(binding.placeText.text.toString(),selectedLat!!,selectedLong!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse))


        }
    }
    private fun handleResponse(){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }
    fun delete(view: View){

        selectedPlace?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()

    }


}