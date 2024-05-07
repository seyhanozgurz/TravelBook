package com.ozgurzorlu.travelbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.ozgurzorlu.travelbook.adapter.PlaceAdapter
import com.ozgurzorlu.travelbook.databinding.ActivityMainBinding
import com.ozgurzorlu.travelbook.model.Place
import com.ozgurzorlu.travelbook.roomdb.PlaceDB
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        val db = Room.databaseBuilder(applicationContext,PlaceDB::class.java,"Places").build()
        val placeDao = db.placeDao()


        compositeDisposable.add(
            placeDao.getAll()
                .subscribeOn(Schedulers.io())   // subscribe olunacak yer
                .observeOn(AndroidSchedulers.mainThread())  //gozlemlenecek yer
                .subscribe(this::handleResponse)
        )
    }

    private fun handleResponse(placeList :List<Place>){
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PlaceAdapter(placeList)
        binding.recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.place_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId ==R.id.addPlace){
            val intent = Intent(this,MapsActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}