package com.misha.drawningapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class MainActivity : AppCompatActivity(), OnClickListener {

    private lateinit var load :Dialog
    private val openGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == RESULT_OK && it.data != null){
            imgBg.setImageURI(it.data?.data)
        }
    }
     @SuppressLint("SuspiciousIndentation")
     private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        permissions->permissions.entries.forEach{
         val permissionName = it.key
         val isGranted = it.value

         if(isGranted){
          val pickIntent = Intent(Intent.ACTION_PICK, Images.Media.EXTERNAL_CONTENT_URI)
             openGallery.launch(pickIntent)
         }

         else
         {
             if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                 Toast.makeText(this, "denied READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show()
             }
         }
     }
     }


    private lateinit var mImageButtonCurrentPaint:ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        drawingView.setSizeForBrush(10F)
        selectBrush.setOnClickListener{
            showBrushDialog()
        }
        clearBtn.setOnClickListener{
            drawingView.clearAll()

        }
        imgButton.setOnClickListener {
            requestStoragePermission()
        }
        undoBtn.setOnClickListener {
            drawingView.undo()
        }
        returnUndoBtn.setOnClickListener {
            drawingView.returnUndo()
        }
        saveButton.setOnClickListener {
            if (isStorageAllowed()) {
                displayLoadScreen()
                lifecycleScope.launch {
                    saveBitmapFile(getBitMapFromView(frameL))
                }
            }
        }
        color0.setOnClickListener(this)
        color1.setOnClickListener(this)
        color2.setOnClickListener(this)
        color3.setOnClickListener(this)
        color4.setOnClickListener(this)
        color5.setOnClickListener(this)
        color6.setOnClickListener(this)
        color7.setOnClickListener(this)
        color8.setOnClickListener(this)

    }


    private fun showBrushDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")

        val smallBtn = brushDialog.smallBrush
        smallBtn.setOnClickListener{
            drawingView.setSizeForBrush(10F)
            brushDialog.dismiss()
        }
        val midBtn = brushDialog.middleBrush
        midBtn.setOnClickListener{
            drawingView.setSizeForBrush(20F)
            brushDialog.dismiss()
        }
        val bigBtn = brushDialog.bigBrush
        bigBtn.setOnClickListener{
            drawingView.setSizeForBrush(30F)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    private fun paint(view:View){
        if (view !== mImageButtonCurrentPaint){
            val imgButton = view as ImageButton
            val colorTag = imgButton.tag.toString()
            drawingView.setColor(colorTag)
            imgButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint = imgButton
        }
    }

    private fun isStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }
    private fun showRationalDialog(title:String, message:String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("Cancel"){
            dialog,_ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationalDialog("Drawing App", "Drawing App needs to access your storage")
        }
        else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun getBitMapFromView(v: View):Bitmap{
        val bitmap = Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bg = v.background
        if(bg != null)
            bg.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)

        v.draw(canvas)
        return bitmap
    }

    private suspend fun saveBitmapFile(mBitmap:Bitmap?):String{
        var res = ""
        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val  file = File(externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp_"+System.currentTimeMillis()/1000+".png")
                    val fileOutput = FileOutputStream(file)
                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()
                    res = file.absolutePath
                    runOnUiThread{
                        load.dismiss()
                        if (res.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved: $res",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(res)
                        }

                        else
                            Toast.makeText(this@MainActivity, "Something went wrong",Toast.LENGTH_SHORT).show()


                    }
                }
                catch (e:Exception){
                    res = ""
                    e.printStackTrace()
                }
            }
        }
        return res
    }
    private fun displayLoadScreen(){
        load = Dialog(this)
        load.setContentView(R.layout.load)
        load.show()
    }
    override fun onClick(v: View?) {
        paint(v!!)
    }

    private fun shareImage(res:String){
        MediaScannerConnection.scanFile(this@MainActivity, arrayOf(res),null){
            _, _ ->
            val values = ContentValues()
            values.put(Images.Media.TITLE, "DrawingApp_${System.currentTimeMillis()/1000}")
            values.put(Images.Media.MIME_TYPE, "image/png")
            val uri = contentResolver.insert(
                Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            val outstream: OutputStream?
            try {
                outstream = contentResolver.openOutputStream(uri!!)
                val icon = getBitMapFromView(frameL)
                icon.compress(Bitmap.CompressFormat.PNG, 100, outstream)
                outstream?.close()
            } catch (e: java.lang.Exception) {
                System.err.println(e.toString())
            }
            val share = Intent()
            share.type = "image/png"
            share.action = Intent.ACTION_SEND
            share.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(share, "Share"))
        }
    }
}