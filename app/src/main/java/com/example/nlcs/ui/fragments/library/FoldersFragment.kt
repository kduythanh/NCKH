package com.example.nlcs.ui.fragments.library

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nlcs.adapter.folder.FolderCopyAdapter
import com.example.nlcs.data.dao.FolderDAO
import com.example.nlcs.data.model.Folder
import com.example.nlcs.databinding.FragmentFoldersBinding
import com.example.nlcs.ui.activities.create.CreateFolderActivity

class FoldersFragment : Fragment() {
    private var binding: FragmentFoldersBinding? = null
    private var folders: ArrayList<Folder>? = null
    private var folderAdapter: FolderCopyAdapter? = null
    private var folderDAO: FolderDAO? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity()
        folderDAO = FolderDAO(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCreateButton()
        setupFolders()
        setupRecyclerView()
    }


    private fun setupCreateButton() {
        binding!!.createSetBtn.setOnClickListener { view1: View? ->
            startActivity(
                Intent(
                    activity, CreateFolderActivity::class.java
                )
            )
        }
    }

    private suspend fun setupFolders() {
        folders = folderDAO!!.getAllFolders()
        if (folders!!.isEmpty()) {
            binding!!.folderCl.visibility = View.VISIBLE
            binding!!.foldersRv.visibility = View.GONE
        } else {
            binding!!.folderCl.visibility = View.GONE
            binding!!.foldersRv.visibility = View.VISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView() {
        folderAdapter = FolderCopyAdapter(requireActivity(), folders!!)
        val linearLayoutManager1 =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        binding!!.foldersRv.layoutManager = linearLayoutManager1
        binding!!.foldersRv.adapter = folderAdapter
        folderAdapter!!.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshData() {
        folders = folderDAO!!.getAllFolders()

        folderAdapter = FolderCopyAdapter(requireActivity(), folders!!)
        binding!!.foldersRv.adapter = folderAdapter
        folderAdapter!!.notifyDataSetChanged()

        if (folders!!.isEmpty()) {
            binding!!.folderCl.visibility = View.VISIBLE
            binding!!.foldersRv.visibility = View.GONE
        } else {
            binding!!.folderCl.visibility = View.GONE
            binding!!.foldersRv.visibility = View.VISIBLE
        }
    }
}