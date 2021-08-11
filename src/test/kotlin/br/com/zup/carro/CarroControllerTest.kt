package br.com.zup.carro

import br.com.zup.CarrosGrpcServiceGrpc
import br.com.zup.CarrosRq
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarroControllerTest(
    val repository: CarroRepository,
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub
    ) {

    @BeforeEach
    internal fun setup() {
        repository.deleteAll()
    }
    @Test
    internal fun `deve adicionar um novo carro`() {
        val response = grpcClient.adicionar(CarrosRq.newBuilder()
            .setModelo("Gol")
            .setPlaca("HPX-1234")
            .build()
        )
        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id)) // garantindo o efeito colateral
        }
    }

    @Test
    internal fun `nao deve adicionar novo carro com placa ja existente`() {
        val existente = repository.save(Carro(modelo = "Palio", placa = "QWE-0987"))

        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarrosRq.newBuilder()
                    .setModelo("Tucson")
                    .setPlaca(existente.placa)
                    .build()
            )
        }
        with(exception) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("carro com placa existente", status.description)
        }
    }

    @Test
    internal fun `nao deve adicionar um novo carro quando dados de entrada forem invalidos`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarrosRq.newBuilder().build())
        }
        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}