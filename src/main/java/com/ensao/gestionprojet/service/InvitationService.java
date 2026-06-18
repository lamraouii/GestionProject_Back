package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.InviteMemberRequestDto;

public interface InvitationService {

    void inviterMembre(Long entrepriseId, InviteMemberRequestDto requestDto);

    void accepterInvitation(Long invitationId);

    void refuserInvitation(Long invitationId);
}
