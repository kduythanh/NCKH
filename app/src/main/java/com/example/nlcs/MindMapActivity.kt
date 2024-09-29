package com.example.nlcs

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.nlcs.databinding.ActivityMindMapBinding
import com.google.firebase.auth.FirebaseAuth

class MindMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMindMapBinding
    private lateinit var neo4jService: Neo4jService
    private val neo4jUri = "bolt+s://f4454805.databases.neo4j.io"
    private val neo4jUser = "neo4j"
    private val neo4jPassword = "T79xAI8tRj6QzvCfiqDMBAlxb4pabJ1UBh_H7qIqlaQ"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMindMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Enable the back arrow
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            updateAllNodesTitles()
            finish()
        }

        // Get the title from intent and set it as the support action bar title
        val mindMapTitle = intent.getStringExtra("mindMapTitle")
        if (mindMapTitle != null) { supportActionBar?.title = mindMapTitle }

        // Initialize Neo4j service
        neo4jService = Neo4jService(neo4jUri, neo4jUser, neo4jPassword)

        // Fetch nodes and display them
        val mindMapID = intent.getStringExtra("mindMapID") ?: return
        fetchAndDisplayAllNodes(mindMapID)

        // Set up the done button click listener
        binding.doneButton.setOnClickListener {
            updateAllNodesTitles()
        }
    }

    // Update all node titles
    private fun updateAllNodesTitles() {
        val nodeUpdates = mutableListOf<Map<String, String>>()
        val parentLayout = binding.zoomableView.findViewById<RelativeLayout>(R.id.mindMapContent)
        // Iterate through each child view of the parent layout
        for (i in 0 until parentLayout.childCount) {
            val nodeView = parentLayout.getChildAt(i) // Get the child view at the current index.
            val nodeTitleEditText = nodeView.findViewById<EditText>(R.id.MindMapNode)

            if (nodeTitleEditText != null) { // Add a null check here
                val newTitle = nodeTitleEditText.text.toString()
                val nodeID = nodeView.getTag(R.id.node_id_tag) as? String
                if (nodeID != null) {
                    nodeUpdates.add(mapOf("nodeID" to nodeID, "newTitle" to newTitle))
                }
            } else {
                Log.e("MindMapActivity", "EditText for node title is null!")
            }
        }

        // Update node titles in a background thread
        Thread {
            neo4jService.updateNodeTitles(nodeUpdates)
            runOnUiThread {
                Toast.makeText(this, "All changes saved!", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    // Fetches and displays all nodes for a given mind map ID
    @SuppressLint("ClickableViewAccessibility")
    private fun fetchAndDisplayAllNodes(mindMapID: String) {
        Thread {
            // Fetch nodes from Neo4j
            val nodes = neo4jService.fetchNodesByMindMapID(mindMapID)

            // Fetch parent-child relationships from Neo4j
            val parentChildMap = neo4jService.fetchParentChildRelationships(mindMapID)

            runOnUiThread {
                val parentLayout = binding.zoomableView.findViewById<RelativeLayout>(R.id.mindMapContent)
                val lineDrawingView = binding.zoomableView.findViewById<LineDrawingView>(R.id.lineDrawingView)

                if (lineDrawingView == null) {
                    Log.e("MindMapActivity", "LineDrawingView is null!")
                    return@runOnUiThread // Early exit to prevent crash
                }

                // Maps to hold node widths and heights
                val nodeWidths = mutableMapOf<String, Int>()
                val nodeHeights = mutableMapOf<String, Int>()

                // Display each node in the mind map
                for (node in nodes) {
                    val nodeView = layoutInflater.inflate(R.layout.mind_map_node, parentLayout, false)
                    val nodeTitleEditText = nodeView.findViewById<EditText>(R.id.MindMapNode)
                    nodeTitleEditText.setText(node["title"] as String)

                    // Store the parent node ID as a tag
                    val parentNodeID = node["nodeID"] as String?
                    nodeView.setTag(R.id.node_id_tag, parentNodeID)

                    // Fetch and set node position (x and y coordinates)
                    val x = (node["x"] as? Float) ?: 0f
                    val y = (node["y"] as? Float) ?: 0f
                    nodeView.x = x
                    nodeView.y = y

                    // Disable default long-click behavior of EditText to ensure custom long-click listener works
                    nodeTitleEditText.setOnLongClickListener {
                        nodeView.performLongClick()
                        true
                    }

                    // Disable default drag and drop behavior of EditText
                    nodeTitleEditText.setOnDragListener { _, _ -> true }

                    // Set up long press listener for context menu
                    nodeView.setOnLongClickListener {
                        showContextMenu(it)
                        true
                    }

                    parentLayout.addView(nodeView)
                    nodeView.post {
                        val width = nodeView.width
                        val height = nodeView.height
                        val id = nodeView.getTag(R.id.node_id_tag) as? String

                        // Store widths and heights in maps
                        if (id != null) {
                            nodeWidths[id] = width
                            nodeHeights[id] = height
                        }

                        // After adding all nodes, pass the updated dimensions to LineDrawingView
                        if (node == nodes.last()) {
                            lineDrawingView.setParentChildMap(parentChildMap, nodes, nodeWidths, nodeHeights)
                        }
                    }

                    // Set up drag and drop listener
                    val dragHandle = nodeView.findViewById<ImageView>(R.id.dragHandle)
                    dragHandle.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val shadowBuilder = View.DragShadowBuilder(nodeView)
                            nodeView.startDragAndDrop(null, shadowBuilder, nodeView, 0)
                            true
                        } else {
                            false
                        }
                    }

                    parentLayout.setOnDragListener { _, event ->
                        when (event.action) {
                            DragEvent.ACTION_DRAG_STARTED -> true

                            DragEvent.ACTION_DRAG_LOCATION -> true

                            DragEvent.ACTION_DROP -> {
                                val draggedView = event.localState as View
                                val dropX = event.x - (draggedView.width / 2)
                                val dropY = event.y - (draggedView.height / 2)
                                draggedView.x = dropX
                                draggedView.y = dropY
                                draggedView.visibility = View.VISIBLE

                                // Update position in the database
                                val nodeID = draggedView.getTag(R.id.node_id_tag) as String
                                updateNodePosition(nodeID, dropX, dropY)

                                // Redraw lines on LineDrawingView after the node has moved
//                                lineDrawingView.invalidate()
                                lineDrawingView.updateNodePosition(nodeID, dropX, dropY)

                                true
                            }

                            DragEvent.ACTION_DRAG_ENDED -> {
                                val draggedView = event.localState as View
                                draggedView.visibility = View.VISIBLE
                                true
                            }
                            else -> false
                        }
                    }
                }
            }
        }.start()
    }


    private fun updateNodePosition(nodeID: String?, x: Float, y: Float) {
        if (nodeID == null) return
        Thread {
            neo4jService.updateNodePositionDB(nodeID, x, y)
        }.start()
    }


    @SuppressLint("InflateParams")
    private fun showContextMenu(view: View) {
        // Inflate the context menu layout
        val contextMenuView = LayoutInflater.from(this).inflate(R.layout.mind_map_context_menu, null)
        val popupWindow = PopupWindow(contextMenuView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)

        // Set up click listeners for each icon in the context menu
        contextMenuView.findViewById<ImageView>(R.id.addChildIcon).setOnClickListener {
            // Get parentNodeID, childTitle, userID, and mindMapID
            val parentNodeID = view.getTag(R.id.node_id_tag) as? String
            val childTitle = "New Child Node"
            val userID = FirebaseAuth.getInstance().currentUser?.uid
            val mindMapID = intent.getStringExtra("mindMapID") ?: return@setOnClickListener

            Thread {
                val newChildNode = neo4jService.addChildNode(parentNodeID, childTitle, userID, mindMapID, 0f, 0f)
                if (newChildNode != null) {
                    runOnUiThread {
                        // Display the new child node in the UI
                        addChildNodeToView(newChildNode)
                        Toast.makeText(this, "Child Node Added", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to add child node", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()

            popupWindow.dismiss()
        }

        // Set up click listener for delete icon
        contextMenuView.findViewById<ImageView>(R.id.deleteChildIcon).setOnClickListener {
            // Get the parent node ID
            val parentNodeID = view.getTag(R.id.node_id_tag) as? String ?: return@setOnClickListener

            Thread {
                // Find the first child of the selected node
                val firstChild = neo4jService.fetchFirstChild(parentNodeID)

                if (firstChild != null) {
                    runOnUiThread {
                        if (firstChild["hasChildren"] as Boolean) {
                            // Show confirmation dialog if deleted node has children
                            AlertDialog.Builder(this)
                                .setTitle("Confirm Deletion")
                                .setMessage("This child has its own children. Do you want to delete this entire branch?")
                                .setPositiveButton("Delete") { _, _ ->
                                    // Delete the first child and its descendants
                                    deleteBranch(firstChild["nodeID"] as String)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        } else {
                            // Directly delete the first child since it is a leaf node
                            deleteLeafNode(firstChild["nodeID"] as String)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "No children to delete.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
            popupWindow.dismiss()
        }

        // Set up click listener for copy icon
        contextMenuView.findViewById<ImageView>(R.id.copyTextIcon).setOnClickListener {
            val nodeText = (view as RelativeLayout).findViewById<EditText>(R.id.MindMapNode).text. toString()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Copied Text", nodeText)
            clipboard.setPrimaryClip(clip)
            popupWindow.dismiss()
        }

        // Show the popup window at the center of the long-pressed view
        popupWindow.elevation = 10f
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0] - 80, location[1] - 100)
    }

    // Add a newly added child node to view
    @SuppressLint("ClickableViewAccessibility")
    private fun addChildNodeToView(node: Map<String, Any>) {
        val parentLayout = binding.zoomableView.findViewById<RelativeLayout>(R.id.mindMapContent)
        val lineDrawingView = binding.zoomableView.findViewById<LineDrawingView>(R.id.lineDrawingView)
        val nodeView = layoutInflater.inflate(R.layout.mind_map_node, parentLayout, false)
        val nodeTitleEditText = nodeView.findViewById<EditText>(R.id.MindMapNode)
        nodeTitleEditText.setText(node["title"] as String)

//        if (lineDrawingView == null) {
//            Log.e("MindMapActivity", "LineDrawingView is null!")
//            return@runOnUiThread // Early exit to prevent crash
//        }

        // Retrieve and set the node ID as a tag to identify the node uniquely
        val nodeID = node["nodeID"] as String?
        nodeView.setTag(R.id.node_id_tag, nodeID)

        // Set node position for newly added nodes
        val x = (node["x"] as? Float)?.toFloat() ?: 0f
        val y = (node["y"] as? Float)?.toFloat() ?: 0f
        nodeView.x = x
        nodeView.y = y
        Log.d("Applied Child Node position", "Node ID: $nodeID, X: $x, Y: $y")

        nodeTitleEditText.setOnLongClickListener {
            // Pass the event to the parent node view
            nodeView.performLongClick()
            true
        }

        nodeTitleEditText.setOnDragListener { _, _ -> true }

        nodeView.setOnLongClickListener {
            showContextMenu(it)
            true
        }

        val dragHandle = nodeView.findViewById<ImageView>(R.id.dragHandle)
        dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val shadowBuilder = View.DragShadowBuilder(nodeView)
                nodeView.startDragAndDrop(null, shadowBuilder, nodeView, 0)
                true
            } else {
                false
            }
        }

        parentLayout.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true

                DragEvent.ACTION_DRAG_LOCATION -> true

                DragEvent.ACTION_DROP -> {
                    val draggedView = event.localState as View
                    val dropX = event.x - (draggedView.width / 2)
                    val dropY = event.y - (draggedView.height / 2)
                    draggedView.x = dropX
                    draggedView.y = dropY
                    draggedView.visibility = View.VISIBLE

                    // Update position in the database
                    val childID = draggedView.getTag(R.id.node_id_tag) as String
                    updateNodePosition(childID, dropX, dropY)

                    lineDrawingView.updateNodePosition(childID, dropX, dropY)
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    val draggedView = event.localState as View
                    draggedView.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }

        parentLayout.addView(nodeView)
    }

    // Delete a node leaf
    private fun deleteLeafNode(nodeID: String) {
        Thread {
            neo4jService.deleteLeafNode(nodeID)
            runOnUiThread {
                Toast.makeText(this, "Node deleted successfully", Toast.LENGTH_SHORT).show()
                refreshMindMap()
            }
        }.start()
    }

    // Delete a branch
    internal fun deleteBranch(nodeID: String) {
        Thread {
            neo4jService.deleteBranch(nodeID)
            runOnUiThread {
                Toast.makeText(this, "Node and its descendants deleted successfully", Toast.LENGTH_SHORT).show()
                refreshMindMap()
            }
        }.start()
    }

    // Refresh the mind map view after deletion
    private fun refreshMindMap() {
        val parentLayout = binding.zoomableView.findViewById<RelativeLayout>(R.id.mindMapContent)
        parentLayout.removeAllViews() // Clear the current nodes

        val mindMapID = intent.getStringExtra("mindMapID") ?: return
        fetchAndDisplayAllNodes(mindMapID) // Fetch and display updated nodes
    }
}