package ar.jornadas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.jornadas.data.Charla

@Composable
fun Catalogo(
    model: ProgramaModel,
    dia: Int,
    elegirDia: (Int) -> Unit,
    tema: String,
    elegirTema: (String) -> Unit,
    abrir: (String) -> Unit,
) {
    val fichas by model.charlas.collectAsStateWithLifecycle()
    val vista by model.vista.collectAsStateWithLifecycle()
    Selector(listOf("Ambos días", "Día 1", "Día 2"), dia, elegirDia)
    val temas = listOf("Todos", "Android", "Datos", "Calidad")
    Selector(temas, temas.indexOf(tema)) { elegirTema(temas[it]) }
    TextButton(onClick = model::actualizar) { Text("Actualizar") }
    if (vista.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (vista.fallo && !vista.hayEdicion) {
        Text("No se pudo consultar el programa")
        OutlinedButton(onClick = model::actualizar) { Text("Reintentar") }
    } else {
        if (vista.fallo) {
            Text("Sin señal: se están usando datos guardados.")
            OutlinedButton(onClick = model::actualizar) { Text("Reintentar") }
        }
        val filtradas = fichas.filter { (dia == 0 || it.dia == dia) && (tema == "Todos" || it.tema == tema) }
        when {
            fichas.isEmpty() && vista.hayEdicion -> Text("Sin charlas programadas")
            fichas.isNotEmpty() && filtradas.isEmpty() -> Text("Ninguna charla coincide con los filtros.")
            else -> Grilla(filtradas, abrir)
        }
    }
}

@Composable
private fun Selector(opciones: List<String>, indice: Int, elegir: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opciones.forEachIndexed { i, texto ->
            FilterChip(selected = i == indice, onClick = { elegir(i) }, label = { Text(texto) })
        }
    }
}

@Composable
fun Grilla(fichas: List<Charla>, abrir: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)) {
        items(fichas, key = { it.id }) { ficha ->
            ElevatedCard(onClick = { abrir(ficha.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}", style = MaterialTheme.typography.labelMedium)
                    Text(ficha.titulo, style = MaterialTheme.typography.titleLarge)
                    Text(if (ficha.favorita) "${ficha.tema} · En mi agenda" else ficha.tema)
                }
            }
        }
    }
}
