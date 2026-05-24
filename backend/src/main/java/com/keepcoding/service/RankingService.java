package com.keepcoding.service;

import com.keepcoding.domain.User;
import com.keepcoding.dto.RankingEntry;
import com.keepcoding.dto.RankingResponse;
import com.keepcoding.dto.RankingRow;
import com.keepcoding.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Constrói o ranking global ordenado por XP e calcula a posição do
 * usuário autenticado quando ele estiver fora do top.
 *
 * Implementação simples (busca todas as linhas e fatia em memória) —
 * adequada à base atual. Para grandes volumes valeria mover o cálculo
 * de posição pra uma query com {@code ROW_NUMBER()}.
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public RankingResponse compute(int topLimit, String userEmail) {
        List<RankingRow> rows = userRepository.findRankingByXp();
        List<RankingEntry> withPosition = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            RankingRow r = rows.get(i);
            withPosition.add(new RankingEntry(
                    i + 1,
                    r.userId(),
                    r.username(),
                    r.xp(),
                    r.easyCount(),
                    r.mediumCount(),
                    r.hardCount()));
        }

        List<RankingEntry> top = withPosition.size() <= topLimit
                ? withPosition
                : withPosition.subList(0, topLimit);

        RankingEntry me = null;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                int myIndex = -1;
                for (int i = 0; i < withPosition.size(); i++) {
                    if (withPosition.get(i).userId().equals(user.getId())) {
                        myIndex = i;
                        break;
                    }
                }
                // Só devolve "me" se o usuário estiver FORA do top exibido.
                if (myIndex >= topLimit) {
                    me = withPosition.get(myIndex);
                }
            }
        }

        return new RankingResponse(top, me, withPosition.size());
    }
}
