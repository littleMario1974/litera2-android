package com.littleMario.litera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littleMario.litera.engine.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.window.Dialog

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
                            if (it.length <= 7) rackEdit.value = it.lowercase()
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
    // LETTER PICKER (FIX: pełny scroll + brak ucinania)
    // -------------------------------------------------
    if (selectedCell.value != null) {

        Dialog(onDismissRequest = { selectedCell.value = null }) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {

                Column(modifier = Modifier.padding(12.dp)) {

                    Text("Wybierz literę")

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        val rows = alphabet.chunked(9)

                        rows.forEach { row ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { ch ->

                                    Button(onClick = {
                                        val (r, c) = selectedCell.value!!

                                        val copy = boardState.value
                                            .map { it.clone() }
                                            .toTypedArray()

                                        copy[r][c] = ch
                                        boardState.value = copy

                                        selectedCell.value = null
                                    }) {
                                        Text(ch.toString())
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            selectedCell.value = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zamknij")
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
                println("🔥 CLICK FIND MOVES")
                val result = solver(boardState.value, rack.value)
                moves.value = result
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("FIND BEST MOVES")
        }

        Text("TOP MOVES:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(moves.value) { move ->
                MoveRow(move)
            }
        }
    }
}
