package br.com.alurafood.pagamentos.service;

import br.com.alurafood.pagamentos.dto.PagamentoDTO;
import br.com.alurafood.pagamentos.http.PedidoClient;
import br.com.alurafood.pagamentos.model.Pagamento;
import br.com.alurafood.pagamentos.model.Status;
import br.com.alurafood.pagamentos.repository.PagamentoRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PagamentoService {

    private PagamentoRepository repository;

    private ModelMapper mapper;

    private PedidoClient pedido;

    private RabbitTemplate rabbitTemplate;

    public Page<PagamentoDTO> findAll(Pageable paginacao) {
        return repository
                .findAll(paginacao).map(p -> mapper.map(p, PagamentoDTO.class));
    }

    public PagamentoDTO findById(Long id) {
        Pagamento pagamento = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Objeto não encontrado"));

        return mapper.map(pagamento, PagamentoDTO.class);
    }

    public PagamentoDTO criarPagamento(PagamentoDTO pagamentoDTO) {
        var pagamento = mapper.map(pagamentoDTO, Pagamento.class);
        pagamento.setStatus(Status.CRIADO);
        repository.save(pagamento);

        return mapper.map(pagamento, PagamentoDTO.class);
    }

    public PagamentoDTO atualizarPagamento(Long id, PagamentoDTO pagamentoDTO) {
        var pagamento = mapper.map(pagamentoDTO, Pagamento.class);
        pagamento.setId(id);
        repository.save(pagamento);

        return mapper.map(pagamento, PagamentoDTO.class);
    }

    public void excluirPagamento(Long id) {
        repository.deleteById(id);
    }

    public void confirmarPagamento(Long id){
        Optional<Pagamento> pagamento = repository.findById(id);

        if (!pagamento.isPresent()) {
            throw new EntityNotFoundException();
        }

        pagamento.get().setStatus(Status.CONFIRMADO);
        repository.save(pagamento.get());
        //pedido.atualizaPagamento(pagamento.get().getPedidoId());
        rabbitTemplate.convertAndSend("pagamento.concluido", pagamento);
    }

    public void alteraStatus(Long id) {
        Optional<Pagamento> pagamento = repository.findById(id);
        if (!pagamento.isPresent()) {
            throw new EntityNotFoundException();
        }

        pagamento.get().setStatus(Status.CONFIRMADO_SEM_INTEGRACAO);
        repository.save(pagamento.get());
    }
}
