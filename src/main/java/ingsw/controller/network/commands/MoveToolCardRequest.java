package ingsw.controller.network.commands;

import ingsw.utilities.ToolCardType;

public class MoveToolCardRequest implements Request {
    public ToolCardType toolCardType;

    public MoveToolCardRequest(ToolCardType toolCardType) {
        this.toolCardType = toolCardType;
    }

    @Override
    public Response handle(RequestHandler requestHandler) {
        return requestHandler.handle(this);
    }
}
