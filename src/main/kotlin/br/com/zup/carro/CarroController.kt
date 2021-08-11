package br.com.zup.carro

import br.com.zup.CarrosGrpcServiceGrpc
import br.com.zup.CarrosRq
import br.com.zup.CarrosRs
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.annotation.Controller
import javax.validation.ConstraintViolationException

@Controller
class CarroController(val repository: CarroRepository ): CarrosGrpcServiceGrpc.CarrosGrpcServiceImplBase() {

    override fun adicionar(request: CarrosRq, responseObserver: StreamObserver<CarrosRs>) {
        if (repository.existsByPlaca(request.placa)) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("carro com placa existente")
                .asRuntimeException())
            return
        }
        val carro = Carro(
            modelo = request.modelo,
            placa = request.placa
        )
        try {
            repository.save(carro)
        } catch (ex: ConstraintViolationException) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("dados de entrada inválidos")
                .asRuntimeException()
            )
            return
        }
        responseObserver.onNext(CarrosRs.newBuilder().setId(carro.id!!).build())
        responseObserver.onCompleted()
    }

}