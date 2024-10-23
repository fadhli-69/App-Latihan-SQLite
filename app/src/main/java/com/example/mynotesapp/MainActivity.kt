package com.example.mynotesapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mynotesapp.adapter.NoteAdapter
import com.example.mynotesapp.databinding.ActivityMainBinding
import com.example.mynotesapp.db.NoteHelper
import com.example.mynotesapp.entity.Note
import com.example.mynotesapp.helper.MappingHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter

    val resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()

    ) { result ->
        if (result.data != null) {
            // Akan dipanggil jika request codenya ADD

            when (result.resultCode) {
                NoteAddUpdateActivity.RESULT_ADD -> {
                    @Suppress("DEPRECATION")
                    val note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getParcelableExtra(NoteAddUpdateActivity.EXTRA_NOTE, Note::class.java)
                    } else {
                        result.data?.getParcelableExtra(NoteAddUpdateActivity.EXTRA_NOTE)
                    }
                    if (note != null) {
                        adapter.addItem(note)
                        binding.rvNotes.smoothScrollToPosition(adapter.itemCount - 1)
                        showSnackbarMessage("Satu item berhasil ditambahkan")
                    }
                }

                NoteAddUpdateActivity.RESULT_UPDATE -> {
                    @Suppress("DEPRECATION")
                    val note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getParcelableExtra(NoteAddUpdateActivity.EXTRA_NOTE, Note::class.java)
                    } else {
                        result.data?.getParcelableExtra(NoteAddUpdateActivity.EXTRA_NOTE)
                    }
                    val position = result.data?.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0)
                    if (note != null && position != null) {
                        adapter.updateItem(position, note)
                        binding.rvNotes.smoothScrollToPosition(position)
                        showSnackbarMessage("Satu item berhasil diubah")
                    }
                }
            }
        }
    }
    companion object {
        private const val EXTRA_STATE = "EXTRA_STATE"
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Notes"

        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.setHasFixedSize(true)

        adapter = NoteAdapter(object : NoteAdapter.OnItemClickCallback {

            override fun onItemClicked(selectedNote: Note?, position: Int) {
                val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_NOTE, selectedNote)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_POSITION, position)
                resultLauncher.launch(intent)
            }
        })
        binding.rvNotes.adapter = adapter
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
            resultLauncher.launch(intent)
        }
        if (savedInstanceState == null) {
            loadNotesAsync()
        } else {
            @Suppress("DEPRECATION")
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelableArrayList(EXTRA_STATE, Note::class.java)
            } else {
                savedInstanceState.getParcelableArrayList(EXTRA_STATE)
            }
            if (list != null) {
                adapter.listNotes = list
            }
        }
    }

    private fun loadNotesAsync() {
        lifecycleScope.launch {
            binding.progressbar.visibility = View.VISIBLE
            val noteHelper = NoteHelper.getInstance(applicationContext)
            noteHelper.open()
            val deferredNotes = async {
                val cursor = noteHelper.queryAll()
                MappingHelper.mapCursorToArrayList(cursor)
            }
            binding.progressbar.visibility = View.INVISIBLE
            val notes = deferredNotes.await()
            if (notes.size > 0) {
                adapter.listNotes = notes
            } else {
                adapter.listNotes = ArrayList()
                showSnackbarMessage("Tidak ada data saat ini")
            }
            noteHelper.close()
        }
    }

    private fun showSnackbarMessage(message: String) {
        Snackbar.make(binding.rvNotes, message, Snackbar.LENGTH_SHORT).show()
    }
}