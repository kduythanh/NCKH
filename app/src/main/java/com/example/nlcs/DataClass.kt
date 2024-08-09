package com.example.nlcs

import java.util.UUID

data class MindMap(
    var id: String? = null,
    var title: String? = null,
    var date: Long = System.currentTimeMillis(),
    var rootNode: Node = Node(id = UUID.randomUUID().toString(), text = "Main Idea", children = listOf())
)

data class Node(
    var id: String? = null,
    var text: String? = null,
    var children: List<Node>? = null
)