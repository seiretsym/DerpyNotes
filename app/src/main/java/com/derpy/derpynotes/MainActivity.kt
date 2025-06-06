package com.derpy.derpynotes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Rect
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.cursoradapter.widget.CursorAdapter
import com.derpy.derpynotes.FeedReaderContract.FeedEntry
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    lateinit var editText: EditText
    lateinit var textView: TextView
    lateinit var mainView: ViewGroup
    lateinit var main: ViewGroup
    lateinit var listView: ListView
    lateinit var historyView: ViewGroup
    lateinit var infoBtn: Button
    lateinit var mainHeader: TextView

    val dbHelper = FeedReaderDbHelper(this)

    fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun insertNote(note: String, fave: Boolean, date: String, id: String) {
        val db = dbHelper.writableDatabase

        if (id.length > 0) {
            val values = ContentValues().apply {
                put(FeedEntry.COLUMN_NAME_NOTE, note)
            }
            val update = db?.update("derpynotes", values, "_id=?", arrayOf(id))
        } else {
            val values = ContentValues().apply {
                put(FeedEntry.COLUMN_NAME_NOTE, note)
                put(FeedEntry.COLUMN_NAME_FAVE, fave)
                put(FeedEntry.COLUMN_NAME_DATE, date)
            }
            val newRowId = db?.insert(FeedEntry.TABLE_NAME, "derpynotes", values)
        }
    }

    fun getNotes(list: ListView){
        val db = dbHelper.readableDatabase

        val projection = arrayOf(BaseColumns._ID, FeedEntry.COLUMN_NAME_NOTE, FeedEntry.COLUMN_NAME_FAVE,
            FeedEntry.COLUMN_NAME_DATE)


// How you want the results sorted in the resulting Cursor
        val sortOrder = "${FeedEntry.COLUMN_NAME_FAVE} DESC, ${BaseColumns._ID} DESC"

        val cursor = db.query(
            FeedEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )

        val cursorAdapter = NoteCursorAdapter(this, cursor, dbHelper, this@MainActivity)
        list.adapter = cursorAdapter

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        textView = findViewById<TextView>(R.id.textView)
        editText = findViewById<EditText>(R.id.editText)
        main = findViewById<ViewGroup>(R.id.main)
        mainView = findViewById<ViewGroup>(R.id.mainFrame)
        historyView = findViewById<ViewGroup>(R.id.historyFrame)
        listView = findViewById<ListView>(R.id.listView)
        infoBtn = findViewById<Button>(R.id.infoBtn)
        mainHeader = findViewById<TextView>(R.id.mainHeader)
        val backBtn = findViewById<Button>(R.id.backBtn)
        editText.setHint("")

        editText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                false
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textView.setText(s.toString())
                var size = textView.textSize
                var sp = size / getResources().getDisplayMetrics().density
                editText.setTextSize(sp)
            }
        })

        editText.setOnTouchListener (object: OnSwipeTouchListener(this) {
            override fun onSwipeLeft(): Boolean {
                if(editText.text.trim().length > 0) {
                    Log.d("LOG", "hello?")
                    val timestamp = DateTimeFormatter
                        .ofPattern("M/dd/yy h:mm a")
                        .withZone(ZoneOffset.systemDefault())
                        .format(Instant.now())
                    insertNote(editText.text.trim().toString(), false, timestamp, editText.hint.toString())
                }
                editText.setText("")
                textView.setText("")
                editText.setHint("")
                mainHeader.setText("Derpy Notes")
                return true
            }
            override fun onSwipeTop(): Boolean {
                hideKeyboard(mainView)
                historyView.isVisible = true
                mainView.isGone = true
                listView.requestFocus()
                getNotes(listView)
                return true
            }
        })

        infoBtn.setOnClickListener (View.OnClickListener() {
            editText.setText("")
            editText.setHint("")
            textView.setText("")
            mainHeader.setText("Derpy Notes")
        })

        backBtn.setOnClickListener (View.OnClickListener() {
            historyView.isGone = true
            mainView.isVisible = true
            editText.requestFocus()
        })

        listView.setOnTouchListener (object: OnSwipeTouchListener(this) {
            override fun onSwipeLeft(): Boolean {
                historyView.isGone = true
                mainView.isVisible = true
                editText.requestFocus()
                return true
            }
        })

        val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            mainView.getWindowVisibleDisplayFrame(r)

            val screenHeight = mainView.rootView.height
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) { // 0.15 is a threshold
                textView.setText(editText.text)
                var size = textView.textSize
                var sp = size / getResources().getDisplayMetrics().density
                editText.setTextSize(sp)
            } else {
                textView.setText(editText.text)
                var size = textView.textSize
                var sp = size / getResources().getDisplayMetrics().density
                editText.setTextSize(sp)
            }
        }

        mainView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        // inset stuff
        ViewCompat.setOnApplyWindowInsetsListener(textView) { v, insets ->
            val i = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()
            )

            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            if (imeVisible) {
                textView.layoutParams.height =  (editText.measuredHeight - imeHeight)
                textView.height = (editText.measuredHeight - imeHeight)
            } else {
                textView.layoutParams.height = (editText.measuredHeight - i.bottom)
                textView.height = (editText.measuredHeight - i.bottom)
                listView.layoutParams.height = (editText.measuredHeight - imeHeight)
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

}

class NoteCursorAdapter(context: Context, cursor: Cursor?, dbHelper: FeedReaderDbHelper, view: MainActivity) : CursorAdapter(context, cursor, 0) {
    val helper = dbHelper
    val db = dbHelper.writableDatabase
    val mV = view.findViewById<ViewGroup>(R.id.mainFrame)
    val hV = view.findViewById<ViewGroup>(R.id.historyFrame)
    val eT = view.findViewById<EditText>(R.id.editText)
    val tV = view.findViewById<TextView>(R.id.textView)
    val lV = view.findViewById<ListView>(R.id.listView)
    val mH = view.findViewById<TextView>(R.id.mainHeader)
    val main = view

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    public override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(R.layout.listview_template, parent, false)
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    public override fun bindView(view: View, context: Context, cursor: Cursor) {
        // Find fields to populate in inflated template
        val note = view.findViewById<View?>(R.id.txtNote) as TextView
        val fave = view.findViewById<View?>(R.id.btnFave) as Button
        val del = view.findViewById<View?>(R.id.btnDel) as Button
        // Extract properties from cursor
        val body: String? = cursor.getString(cursor.getColumnIndexOrThrow("note"))
        var star: Int? = cursor.getInt(cursor.getColumnIndexOrThrow("fave"))
        val id: Int = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
        val date: String? = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"))
        // Populate fields with extracted properties
        note.setText(body)
        if (star != 0) {
            fave.setText("★")
        } else {
            fave.setText("☆")
        }

        del.setOnClickListener(View.OnClickListener() {
                db.beginTransaction()
                try {
                    val result = db.delete("derpynotes", "_id=?", arrayOf(id.toString()))
                    if (result != 0) {

                        val projection = arrayOf(
                            BaseColumns._ID, FeedEntry.COLUMN_NAME_NOTE, FeedEntry.COLUMN_NAME_FAVE,
                            FeedEntry.COLUMN_NAME_DATE
                        )

// How you want the results sorted in the resulting Cursor
                        val sortOrder =
                            "${FeedEntry.COLUMN_NAME_FAVE} DESC, ${BaseColumns._ID} DESC"

                        val cursor = db.query(
                            FeedEntry.TABLE_NAME,   // The table to query
                            projection,             // The array of columns to return (pass null to get all)
                            null,              // The columns for the WHERE clause
                            null,          // The values for the WHERE clause
                            null,                   // don't group the rows
                            null,                   // don't filter by row groups
                            sortOrder               // The sort order
                        )

                        val cursorAdapter =
                            NoteCursorAdapter(context, cursor, helper, main)
                        lV.adapter = cursorAdapter
                        db.setTransactionSuccessful()
                    }
                } finally {
                    db.endTransaction()
                }
        })


        note.setOnClickListener (View.OnClickListener {
            tV.setText(body)
            eT.setText(body)
            eT.setHint(id.toString())
            eT.requestFocus()
            mH.setText(date)
            mV.isVisible = true
            hV.isGone = true
        })

        fave.setOnClickListener (View.OnClickListener() {
                db.beginTransaction()
                try {
                    var t = false
                    if (star == 0) {
                        t = true
                    }
                    val values = ContentValues()
                    values.put(FeedEntry.COLUMN_NAME_FAVE, t)
                    val result = db.update("derpynotes", values, "_id=?", arrayOf(id.toString()))
                    if (result != 0) {
                        if (star != 0) {
                            fave.setText("☆")
                            star = 0
                        } else {
                            fave.setText("★")
                            star = 1
                        }
                        db.setTransactionSuccessful()
                    }
                } finally {
                    db.endTransaction()
                }
        })
    }
}

open class OnSwipeTouchListener(context: Context) : View.OnTouchListener {

     val gestureDetector = GestureDetector(context, GestureListener())

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 75
        private val SWIPE_VELOCITY_THRESHOLD = 75

        override fun onDown(e1: MotionEvent): Boolean {
            return false
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            var result = false
            try {
                val diffY = e2.y - e1!!.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                           result = onSwipeRight()
                        } else {
                           result = onSwipeLeft()
                        }
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        result = onSwipeBottom()
                    } else {
                        result = onSwipeTop()
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }
    }

    open fun onSwipeLeft(): Boolean { return false }

    open fun onSwipeRight(): Boolean { return false }

    open fun onSwipeTop(): Boolean { return false }

    open fun onSwipeBottom(): Boolean { return false }
}

// sqlite stuff
object FeedReaderContract {
    // Table contents are grouped together in an anonymous object.
    object FeedEntry : BaseColumns {
        const val TABLE_NAME = "derpynotes"
        const val COLUMN_NAME_NOTE = "note"
        const val COLUMN_NAME_FAVE = "fave"
        const val COLUMN_NAME_DATE = "timestamp"
    }
}

private const val SQL_CREATE_ENTRIES =
    "CREATE TABLE ${FeedEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${FeedEntry.COLUMN_NAME_NOTE} TEXT," +
            "${FeedEntry.COLUMN_NAME_FAVE} BOOLEAN," +
            "${FeedEntry.COLUMN_NAME_DATE} TEXT)"

private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FeedEntry.TABLE_NAME}"

open class FeedReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "derpynotes.db"
    }
}