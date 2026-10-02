package com.example.bookmarks.data

data class FolderNode(val folder: Folder, val depth: Int)

/** All folders in depth-first order (each folder followed by its subfolders), sorted by name. */
fun flattenTree(folders: List<Folder>): List<FolderNode> {
    val children = folders.groupBy { it.parentId }
    val result = mutableListOf<FolderNode>()
    fun visit(parentId: Long?, depth: Int) {
        children[parentId].orEmpty().sortedBy { it.name.lowercase() }.forEach {
            result += FolderNode(it, depth)
            visit(it.id, depth + 1)
        }
    }
    visit(null, 0)
    return result
}

/** The ids of [folderId] and every folder nested inside it. */
fun descendantsAndSelf(folderId: Long, folders: List<Folder>): Set<Long> {
    val children = folders.groupBy { it.parentId }
    val result = mutableSetOf(folderId)
    val queue = ArrayDeque(listOf(folderId))
    while (queue.isNotEmpty()) {
        children[queue.removeFirst()].orEmpty().forEach {
            if (result.add(it.id)) queue.add(it.id)
        }
    }
    return result
}

/** The folders from the top level down to [folderId] (inclusive); empty for the top level itself. */
fun pathTo(folderId: Long?, foldersById: Map<Long, Folder>): List<Folder> {
    val path = ArrayDeque<Folder>()
    var current = folderId?.let(foldersById::get)
    while (current != null && path.size < foldersById.size) {
        path.addFirst(current)
        current = current.parentId?.let(foldersById::get)
    }
    return path.toList()
}
