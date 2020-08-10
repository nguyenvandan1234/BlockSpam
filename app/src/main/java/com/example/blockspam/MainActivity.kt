package com.example.blockspam

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.provider.BlockedNumberContract.BlockedNumbers
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity


class MainActivity : AppCompatActivity() {
    val REQUEST_CODE_SET_DEFAULT_DIALER=200
    lateinit var  telecomManager : TelecomManager;
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        if (applicationContext.packageName != this.telecomManager.getDefaultDialerPackage()) {
            try {
                setDefaultDialer(this.telecomManager)
            } catch (unused: Exception) {
            }
        } else {
            getAllNumberBlock()
//            putNumberBlock(applicationContext, "1234")
            //startActivity(telecomManager.createManageBlockedNumbersIntent(), null);
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun putNumberBlock(context : Context, number : String) {
        val values = ContentValues()
        values.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
        val uri: Uri? = contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun removeNumberBlock(context: Context, number: String) {
        val values = ContentValues()
        values.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
        val uri = contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
        contentResolver.delete(uri!!, null, null)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getAllNumberBlock() {
        val c: Cursor? = contentResolver.query(BlockedNumbers.CONTENT_URI, arrayOf(BlockedNumbers.COLUMN_ID,
                BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                BlockedNumbers.COLUMN_E164_NUMBER), null, null, null)
        if (c != null) {
            c.moveToFirst()
            do {
                Log.d("dannvb", c.getString(1))
            }
            while (c.moveToNext())
        };

    }

    fun setDefaultDialer(telecomManager2: TelecomManager) {
        if (VERSION.SDK_INT >= 24 && packageName != telecomManager2.defaultDialerPackage) {
            startActivityForResult(
                Intent("android.telecom.action.CHANGE_DEFAULT_DIALER").putExtra(
                    "android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME",
                    packageName
                ), 100
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SET_DEFAULT_DIALER -> checkSetDefaultDialerResult(resultCode)
        }
    }

    private fun checkSetDefaultDialerResult(resultCode: Int) {
        val message = when (resultCode) {
            RESULT_OK       -> "User accepted request to become default dialer"
            RESULT_CANCELED -> "User declined request to become default dialer"
            else            -> "Unexpected result code $resultCode"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    }
}