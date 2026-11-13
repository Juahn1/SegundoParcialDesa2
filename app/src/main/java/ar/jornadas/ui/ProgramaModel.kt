package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.JornadasApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class Vista(val cargando: Boolean, val fallo: Boolean, val hayEdicion: Boolean)

class ProgramaModel(app: Application) : AndroidViewModel(app) {
    private val servicios = (app as JornadasApp).servicios
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val vista = combine(repo.cargando, repo.fallo, repo.hayEdicion) { c, f, h -> Vista(c, f, h) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Vista(false, false, false))

    fun iniciarSesion() = repo.cargaAutomatica()
    fun actualizar() = repo.actualizar()
    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
