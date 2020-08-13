package com.example.blockspam

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    val REQUEST_CODE_SET_DEFAULT_DIALER = 200
    val RESULT_CHOSE_CONTACT = 1122;
    lateinit var  telecomManager : TelecomManager;
    lateinit var mListNumber : MutableList<String>;

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mListNumber = mutableListOf<String>()
        numberAdapter = RowAdapter(applicationContext, mListNumber)
        lv_main_activity_list.adapter = numberAdapter
        this.telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        if (applicationContext.packageName != this.telecomManager.getDefaultDialerPackage()) {
            try {
                setDefaultDialer(this.telecomManager)
            } catch (unused: Exception) {
            }
        } else {
            getAllNumberBlock()
            intClick()
        }
    }

    fun intClick() {

        // nut them tu callog
        fab_insert_from_callog.setOnClickListener(View.OnClickListener {
            val strFields = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER
            )
            val strOrder = CallLog.Calls.DATE + " DESC"
            val cursorCall = contentResolver.query(
                CallLog.Calls.CONTENT_URI, strFields,
                null, null, strOrder
            )
            var listCall : MutableList<String>
            listCall = mutableListOf<String>()

            if (cursorCall != null && cursorCall.count > 0) {
                while (cursorCall.moveToNext()) {
                    if (!listCall.contains(cursorCall.getString(1))) {
                        listCall.add(cursorCall.getString(1))
                    }
                }
            }
            val arrayAdapter = ArrayAdapter<String>(
                this@MainActivity,
                android.R.layout.select_dialog_item
            )


            for (s in listCall) {
                arrayAdapter.add(s)
            }
            val builderSingle: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
            builderSingle.setIcon(R.drawable.ic_baseline_call_24)
            builderSingle.setTitle("Select One Name:-")
            builderSingle.setAdapter(arrayAdapter) { dialog, which ->
                var number = arrayAdapter.getItem(which)
                Toast.makeText(applicationContext, number, Toast.LENGTH_LONG).show()
                if (number != null) {
                    putNumberBlock(applicationContext, number)
                    mListNumber.add(number)
                    numberAdapter.notifyDataSetChanged()
                };
            }
            builderSingle.show()
        })

        // nut them tu contact
        fab_insert_from_contact.setOnClickListener(View.OnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(intent, RESULT_CHOSE_CONTACT)
        })

        fab_insert_from_mess.setOnClickListener(View.OnClickListener {
            getAllSms()
        })

        fab_insert_from_number.setOnClickListener(View.OnClickListener {
            addInputNumber()
        })
    }

    private fun getSMSCOnversationlist() {
        val SMS_INBOX = Uri.parse("content://sms/conversations/")
        val c: Cursor =
            applicationContext.getContentResolver().query(SMS_INBOX, null, null, null, "date desc")!!
        val count = arrayOfNulls<String>(c.count)
        val snippet = arrayOfNulls<String>(c.count)
        val thread_id = arrayOfNulls<String>(c.count)
        c.moveToFirst()
        for (i in 0 until c.count) {
            try {
                for (i in 0..100) {
                    Log.d("dannvb", c.getColumnName(i) + " " + c.getString(i))
                }
            } catch (e :Exception) {

            }
            count[i] = c.getString(c.getColumnIndexOrThrow("msg_count")).toString()
            thread_id[i] = c.getString(c.getColumnIndexOrThrow("thread_id")).toString()
            snippet[i] = c.getString(c.getColumnIndexOrThrow("snippet")).toString()
            c.moveToNext()
        }
        c.close()
    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun getAllNumberBlock() {
        val c: Cursor? = contentResolver.query(BlockedNumbers.CONTENT_URI, arrayOf(BlockedNumbers.COLUMN_ID,
                BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                BlockedNumbers.COLUMN_E164_NUMBER), null, null, null)
        if (c != null && c.count > 0) {
            c.moveToFirst()
            do {
                mListNumber.add(c.getString(1))
                numberAdapter.notifyDataSetChanged()
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
            RESULT_CHOSE_CONTACT -> if (data != null) { getResultChoseContact(data) }
        }
    }

    fun getResultChoseContact(data : Intent) {
        var result = data.data;
        val cursor = contentResolver.query(
            result!!, null,
            null, null, null
        )
        cursor!!.moveToFirst()
        val id: String = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))

        val hasPhone: String =
            cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))

        val nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val name = cursor.getString(nameColumnIndex)
        if (hasPhone.equals("1")) {
            val phones = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                null, null
            )
            phones!!.moveToFirst()
            val phoneNumber = phones.getString(phones.getColumnIndex("data1"))
            putNumberBlock(applicationContext, phoneNumber)
            mListNumber.add(phoneNumber)
            numberAdapter.notifyDataSetChanged()
        } else {
            Toast.makeText(applicationContext, "Contact not have number" , Toast.LENGTH_LONG).show()
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

    private fun addInputNumber() {
        val builder =
            AlertDialog.Builder(this)
        builder.setTitle("Title")

        val input = EditText(this)
        input.setInputType(InputType.TYPE_CLASS_NUMBER)
        builder.setView(input)
        builder.setNegativeButton(android.R.string.yes) { _,_ ->
            putNumberBlock(applicationContext, input.text.toString())
            mListNumber.add(input.text.toString());
            numberAdapter.notifyDataSetChanged();
        }

        builder.setPositiveButton(android.R.string.cancel) { _,_ ->
            Toast.makeText(applicationContext,
                android.R.string.cancel, Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    fun getAllSms(): List<String>? {
        var listNumber : MutableList<String>;
        listNumber = mutableListOf()
        val lstSms: List<String> = ArrayList<String>()
        val message = Uri.parse("content://sms/")
        val cr: ContentResolver = applicationContext.getContentResolver()
        val c: Cursor? = cr.query(message, null, null, null, null)
        if (c != null && c.moveToFirst()) {
            val totalSMS = c.count
            for (i in 0 until totalSMS) {
                try {
                    var s = c.getString(c.getColumnIndexOrThrow("address"))
                    try {
                        if (s.get(1).toInt() > 0) {
                            if (!listNumber.contains(s)) {
                                listNumber.add(s)
                            }
                        }
                    } catch (e: Exception) {

                    }
                } catch (e : Exception) {

                }
                c.moveToNext()
            }
        }
        if (listNumber.size > 0) {
            val arrayAdapter = ArrayAdapter<String>(
                this@MainActivity,
                android.R.layout.select_dialog_item
            )


            for (s in listNumber) {
                arrayAdapter.add(s)
            }
            val builderSingle: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
            builderSingle.setIcon(R.drawable.ic_baseline_call_24)
            builderSingle.setTitle("Select One Name:-")
            builderSingle.setAdapter(arrayAdapter) { dialog, which ->
                var number = arrayAdapter.getItem(which)
                Toast.makeText(applicationContext, number, Toast.LENGTH_LONG).show()
                if (number != null) {
                    putNumberBlock(applicationContext, number)
                    mListNumber.add(number)
                    numberAdapter.notifyDataSetChanged()
                };
            }
            builderSingle.show()
        }
        // else {
        // throw new RuntimeException("You have no SMS");
        // }
        if (c != null) {
            c.close()
        }
        return lstSms
    }
    companion object {
        lateinit var numberAdapter: RowAdapter;

        @RequiresApi(Build.VERSION_CODES.N)
        fun putNumberBlock(context : Context, number : String) {
            val values = ContentValues()
            values.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            val uri: Uri? = context.contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun removeNumberBlock(context: Context, number: String) {
            val values = ContentValues()
            values.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            val uri = context.contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
            context.contentResolver.delete(uri!!, null, null)
        }
    }


    class RowAdapter(context: Context, val list: MutableList<String>) : ArrayAdapter<String>(context, R.layout.row_number) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = LayoutInflater.from(context)
            val rowView = inflater.inflate(R.layout.row_number, null, true)

            val titleText = rowView.findViewById(R.id.tv_row_number_number) as TextView
            val imageView = rowView.findViewById(R.id.iv_row_number_remove) as ImageView
            titleText.text = list[position]
            imageView.setOnClickListener(View.OnClickListener {
                if (VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    removeNumberBlock(context, list[position])
                    list.removeAt(position);
                    numberAdapter.notifyDataSetChanged()
                }
            })
            return rowView
        }

        override fun getCount(): Int {
            return list.size
        }
    }
}