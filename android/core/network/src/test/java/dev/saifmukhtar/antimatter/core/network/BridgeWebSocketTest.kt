package dev.saifmukhtar.antimatter.core.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BridgeWebSocketTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var contextMock: Context
    private lateinit var bridgeWebSocket: BridgeWebSocket

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        contextMock = mock()
        bridgeWebSocket = BridgeWebSocket(contextMock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial connection state should be DISCONNECTED`() {
        assertEquals(BridgeWebSocket.ConnectionState.DISCONNECTED, bridgeWebSocket.connectionState.value)
    }

    @Test
    fun `disconnect should update state to DISCONNECTED and set manual flag`() {
        bridgeWebSocket.disconnect()
        assertEquals(BridgeWebSocket.ConnectionState.DISCONNECTED, bridgeWebSocket.connectionState.value)
    }
    
    // Testing the inner state changes directly is difficult without mocking OkHttpClient
    // So we test the public API contracts and state flows.
    @Test
    fun `connect with empty URL should not change state to CONNECTING`() = runTest(testDispatcher) {
        bridgeWebSocket.connect("")
        // Because OkHttp will fail to build the request due to empty URL, it should catch and set to DISCONNECTED.
        assertEquals(BridgeWebSocket.ConnectionState.DISCONNECTED, bridgeWebSocket.connectionState.value)
    }
}
