package org.ftn.dto;

import org.ftn.constant.Vote;

public record VoteResponse(Vote vote,
                           Object body) {
}
