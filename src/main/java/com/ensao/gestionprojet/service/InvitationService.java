package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.InviteMemberRequestDto;
import com.ensao.gestionprojet.dto.InvitationResponseDto;

import java.util.List;

public interface InvitationService {

    void inviterMembre(Long entrepriseId, InviteMemberRequestDto requestDto);

    void accepterInvitation(Long invitationId);

    void refuserInvitation(Long invitationId);

    List<InvitationResponseDto> getMesInvitations();

    void accepterInvitation(String invitationKey);

    void refuserInvitation(String invitationKey);
}
