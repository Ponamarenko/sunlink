package com.sunlink.svitgrid.integration

import com.sunlink.svitgrid.data.model.Result
import com.sunlink.svitgrid.data.model.error
import com.sunlink.svitgrid.data.repository.DataRepository
import com.sunlink.svitgrid.viewmodel.ConnectionDiagnostic
import com.sunlink.svitgrid.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Stage 25B — диагностика подключения без сети и без реального backend. */
@OptIn(ExperimentalCoroutinesApi::class)
class Stage25BConnectionTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private class FailingRepo(private val exception: Exception?) : DataRepository(null) {
        override suspend fun loadStatus(): Result<com.sunlink.svitgrid.data.model.InverterStatus> {
            if (exception != null) throw exception
            return error(Exception("HTTP 500"))
        }
    }

    private suspend fun waitFor(vm: MainViewModel) {
        val start = System.currentTimeMillis()
        while (vm.refreshing.value && System.currentTimeMillis() - start < 4000) delay(20)
    }

    @Test
    fun `tls error is classified with friendly message`() = runBlocking {
        val vm = MainViewModel(FailingRepo(Exception("javax.net.ssl.SSLHandshakeException: certificate expired")))
        vm.checkConnection()
        waitFor(vm)
        val diagnostic = vm.connectionDiagnostic.value
        assertNotNull(diagnostic)
        assertEquals("TLS_ERROR", diagnostic!!.state)
        assertTrue(diagnostic.message.contains("TLS", true))
        assertFalse(diagnostic.message.contains("Exception"))
    }

    @Test
    fun `dns failure is classified`() = runBlocking {
        val vm = MainViewModel(FailingRepo(Exception("java.net.UnknownHostException: Unable to resolve host")))
        vm.checkConnection()
        waitFor(vm)
        assertEquals("DNS_FAILURE", vm.connectionDiagnostic.value!!.state)
    }

    @Test
    fun `timeout is classified`() = runBlocking {
        val vm = MainViewModel(FailingRepo(Exception("java.net.SocketTimeoutException: timeout")))
        vm.checkConnection()
        waitFor(vm)
        assertEquals("TIMEOUT", vm.connectionDiagnostic.value!!.state)
    }

    @Test
    fun `http error is classified`() = runBlocking {
        val vm = MainViewModel(FailingRepo(null))
        vm.checkConnection()
        waitFor(vm)
        assertEquals("HTTP_ERROR", vm.connectionDiagnostic.value!!.state)
    }

    @Test
    fun `diagnostic contains configured url and https flag`() = runBlocking {
        val vm = MainViewModel(FailingRepo(Exception("timeout")))
        vm.checkConnection()
        waitFor(vm)
        val d: ConnectionDiagnostic = vm.connectionDiagnostic.value!!
        assertTrue(d.url.startsWith("https://"))
        assertTrue(d.https)
        assertNotNull(d.responseTimeMs)
        assertNotNull(d.checkedAtUtc)
    }
}
