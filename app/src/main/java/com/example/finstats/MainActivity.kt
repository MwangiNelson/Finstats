package com.example.finstats

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this);

        val requestCodeReadSms = 1 // This can be any integer
        val appPermission = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS)

        ActivityCompat.requestPermissions(this, appPermission, requestCodeReadSms)

        val smsButton = findViewById<Button>(R.id.read_sms)

        val documentOwnerInputField = findViewById<EditText>(R.id.doc_owner)

        smsButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED
            ) {

                val documentOwner = documentOwnerInputField.text.toString().trim { it <= ' ' }

                if (TextUtils.isEmpty(documentOwner)){
                    Toast.makeText(this@MainActivity, "Please enter a document title", Toast.LENGTH_LONG).show()
                }else  {
                    Toast.makeText(this@MainActivity, "Collectioncreated under: $documentOwner", Toast.LENGTH_LONG).show()

                    readAllSmsFromFirstSender(documentOwner)
                }
            }
        }
    }

    private fun readAllSmsFromFirstSender(documentOwner: String) {
        val cursor: Cursor? = contentResolver.query(
            Uri.parse("content://sms/inbox"),
            null,
            null,
            null,
            null
        )

        val linearLayout = findViewById<LinearLayout>(R.id.linear_layout)
        var firstSender: String? = null

        if (cursor?.moveToFirst() == true) {
            val indexBody = cursor.getColumnIndex("body")
            val indexAddress = cursor.getColumnIndex("address")

            firstSender = cursor.getString(indexAddress)

            val conversationCursor: Cursor? = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                null,
                "address=?",
                arrayOf(firstSender),
                null
            )

            // Get the Firestore instance
            val db = FirebaseFirestore.getInstance()

            if (conversationCursor?.moveToFirst() == true) {
                var counter = 0;
                val conversationIndexBody = conversationCursor.getColumnIndex("body")
                val conversationIndexAddress = conversationCursor.getColumnIndex("address")

                do {
                    val message = conversationCursor.getString(conversationIndexBody)
                    val address = conversationCursor.getString(conversationIndexAddress)
                    counter += 1;


                    if(counter < 30){
                        val textView = TextView(this)
                        textView.text = "From: $address\nMessage: $message\n\n"
                        linearLayout.addView(textView)
                    }


                    // Create a new message document with a sender's address and message body
                    val sms = hashMapOf(
                        "address" to address,
                        "body" to message,
                        "label" to ""
                    )

                    Toast.makeText(this@MainActivity, "Upload testing", Toast.LENGTH_SHORT).show()


                    // Add the message document to the Firestore custom collection
                    db.collection(documentOwner)
                        .add(sms)
                        .addOnSuccessListener { documentReference ->
                            // Successfully uploaded the message
                            println("Message uploaded with ID: ${documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            // Failed to upload the message
                            println("Error uploading message: $e")
                        }

                } while (conversationCursor.moveToNext())
            }
            conversationCursor?.close()
        }
        cursor?.close()
    }


    fun testSend() {

        Toast.makeText(this@MainActivity, "Upload testing", Toast.LENGTH_SHORT).show()

        val db = FirebaseFirestore.getInstance()
        val sms = hashMapOf(
            "address" to "test",
            "body" to "test"
        )

        db.collection("NelsonStats")
            .add(sms)
            .addOnSuccessListener { documentReference ->
                // Successfully uploaded the message
                Toast.makeText(this@MainActivity, "Upload successful", Toast.LENGTH_SHORT).show()
                println("Message uploaded with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                // Failed to upload the message
                Toast.makeText(this@MainActivity, "Upload failure", Toast.LENGTH_SHORT).show()
                println("Error uploading message: $e")
            }
    }



}
