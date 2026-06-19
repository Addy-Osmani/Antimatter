package dev.saifmukhtar.antimatter.feature.connect

import dev.saifmukhtar.antimatter.core.data.UserPreferencesRepository
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var webSocketMock: BridgeWebSocket
    private lateinit var userPrefsMock: UserPreferencesRepository
    private lateinit var viewModel: ConnectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        webSocketMock = mock()
        userPrefsMock = mock()

        val mockStateFlow = MutableStateFlow(BridgeWebSocket.ConnectionState.DISCONNECTED)
        whenever(webSocketMock.connectionState).thenReturn(mockStateFlow)
        
        val mockCredsFlow = MutableStateFlow(dev.saifmukhtar.antimatter.core.data.Credentials(null, null, null, null, null))
        whenever(userPrefsMock.savedCredentialsFlow).thenReturn(mockCredsFlow)

        val mockLoadedFlow = MutableStateFlow(true)
        whenever(userPrefsMock.isLoadedFlow).thenReturn(mockLoadedFlow)

        val mockProfilesFlow = MutableStateFlow<List<dev.saifmukhtar.antimatter.core.data.GatewayProfile>>(emptyList())
        whenever(userPrefsMock.profilesFlow).thenReturn(mockProfilesFlow)

        viewModel = ConnectionViewModel(webSocketMock, userPrefsMock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `connectManually should save credentials and connect`() = runTest(testDispatcher) {
        viewModel.connectManually("https://test.com", "cfid", "cfsec", "token", "pubkey")
        advanceUntilIdle()
        
        verify(userPrefsMock).saveCredentials("https://test.com", "cfid", "cfsec", "token", "pubkey")
        verify(webSocketMock).connect("https://test.com", "cfid", "cfsec", "token", "pubkey")
    }

    @Test
    fun `disconnectManually should clear credentials and disconnect`() = runTest(testDispatcher) {
        viewModel.disconnectManually()
        advanceUntilIdle()
        
        verify(userPrefsMock).clearCredentials()
        verify(webSocketMock).disconnect()
    }
}
