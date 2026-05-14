package com.littleMario.litera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.littleMario.litera.engine.*

@Composable
fun MoveRow(move: Move) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("WORD: ${move.word}")
            Text("POS: (${move.row}, ${move.col})")
            Text("DIR: ${move.direction}")
            Text("SCORE: ${move.score}")
        }
    }
}

@Composable
fun LiterakiScreen(
    boardState: MutableState<Array<Array<Char?>>>,
    rack: MutableState<MutableList<Char>>,
    solver: (Array<Array<Char?>>, List<Char>) -> List<Move>
) {

    val selectedCell = remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val moves = remember { mutableStateOf(emptyList<Move>()) }

    val rackEdit = remember { mutableStateOf("") }
    val showRackDialog = remember { mutableStateOf(false) }

    // 🔥 NOWE: tryb wpisywania słowa
    val wordInput = remember { mutableStateOf("") }
    val direction = remember { mutableStateOf("H") }

    val debugMode = remember { mutableStateOf(false) }

    val alphabet = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    // -------------------------------------------------
    // RACK EDIT
    // -------------------------------------------------
    if (showRackDialog.value) {

        AlertDialog(
            onDismissRequest = { showRackDialog.value = false },
            title = { Text("Wpisz 7 liter") },
            text = {
                Column {

                    TextField(
                        value = rackEdit.value,
                        onValueChange = {
                            if (it.length <= 7) {
                                rackEdit.value = it.lowercase()
                            }
                        },
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(onClick = {

                        val chars = rackEdit.value
                            .filter { alphabet.contains(it) }
                            .take(7)
                            .padEnd(7, ' ')
                            .toList()

                        rack.value = chars.toMutableList()
                        showRackDialog.value = false
                    }) {
                        Text("ZATWIERDŹ")
                    }
                }
            },
            confirmButton = {}
        )
    }

    // -------------------------------------------------
    // LETTER / WORD INPUT DIALOG
    // -------------------------------------------------
    if (selectedCell.value != null) {

        Dialog(onDismissRequest = { selectedCell.value = null }) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {

                Column(modifier = Modifier.padding(12.dp)) {

                    Text("Wpisz słowo lub ustaw literę")

                    Spacer(Modifier.height(8.dp))

                    // 🔥 TRYB SŁOWA
                    TextField(
                        value = wordInput.value,
                        onValueChange = {
                            wordInput.value = it.lowercase()
                        },
                        label = { Text("Słowo") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Row {

                        Button(onClick = { direction.value = "H" }) {
                            Text("→ H")
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(onClick = { direction.value = "V" }) {
                            Text("↓ V")
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(onClick = {

                            val (r, c) = selectedCell.value!!

                            val copy = boardState.value
                                .map { it.clone() }
                                .toTypedArray()

                            val word = wordInput.value

                            for (i in word.indices) {

                                val rr = if (direction.value == "V") r + i else r
                                val cc = if (direction.value == "H") c + i else c

                                if (rr in 0..14 && cc in 0..14) {
                                    copy[rr][cc] = word[i]
                                }
                            }

                            boardState.value = copy
                            selectedCell.value = null
                            wordInput.value = ""

                        }) {
                            Text("WSTAW SŁOWO")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 🔥 USUŃ
                    Button(
                        onClick = {
                            val (r, c) = selectedCell.value!!
                            val copy = boardState.value.map { it.clone() }.toTypedArray()
                            copy[r][c] = null
                            boardState.value = copy
                            selectedCell.value = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("USUŃ LITERĘ")
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { selectedCell.value = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ZAMKNIJ")
                    }
                }
            }
        }
    }

    // -------------------------------------------------
    // MAIN UI
    // -------------------------------------------------
    Column(modifier = Modifier.fillMaxSize()) {

        BoardView(boardState.value) { r, c ->
            selectedCell.value = r to c
        }

        Spacer(Modifier.height(8.dp))

        Text("RACK:", style = MaterialTheme.typography.titleMedium)

        Row(Modifier.padding(8.dp)) {

            rack.value.forEach {
                Text(it.toString(), Modifier.padding(6.dp))
            }

            Spacer(Modifier.width(12.dp))

            Button(onClick = {
                rackEdit.value = rack.value.joinToString("")
                showRackDialog.value = true
            }) {
                Text("EDIT RACK")
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val result = solver(boardState.value, rack.value)
                moves.value = result
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("FIND BEST MOVES")
        }

        Button(
            onClick = { debugMode.value = !debugMode.value },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(if (debugMode.value) "DEBUG ON" else "DEBUG OFF")
        }

        Spacer(Modifier.height(8.dp))

        Text("TOP MOVES:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            items(moves.value) { move ->
                MoveRow(move)
            }
        }
    }
}