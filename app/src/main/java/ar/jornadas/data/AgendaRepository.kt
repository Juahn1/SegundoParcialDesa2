package ar.jornadas.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AgendaRepository(
    private val api: ProgramaApi,
    private val dao: CarteleraDao,
    private val alcance: CoroutineScope,
) {
    val programa = dao.observar()
    val hayEdicion: Flow<Boolean> = dao.observarEdicion().map { it?.descargada == true }

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()
    private val _fallo = MutableStateFlow(false)
    val fallo: StateFlow<Boolean> = _fallo.asStateFlow()

    private val automatica = AtomicBoolean(false)
    private val enCurso = AtomicBoolean(false)

    // Una carga automática por proceso.
    fun cargaAutomatica() {
        if (automatica.compareAndSet(false, true)) actualizar()
    }

    // Una petición a la vez: los toques extra durante la espera no suman intentos.
    fun actualizar() {
        if (!enCurso.compareAndSet(false, true)) return
        _cargando.value = true
        alcance.launch {
            try {
                val fichas = api.consultar().map {
                    Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
                }
                dao.reemplazarPrograma(fichas)
                _fallo.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _fallo.value = true
            } finally {
                _cargando.value = false
                enCurso.set(false)
            }
        }
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
