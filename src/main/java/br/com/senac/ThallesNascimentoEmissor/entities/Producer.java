package br.com.senac.ThallesNascimentoEmissor.entities;

import java.io.Serializable;

public class Producer implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nameUser;
    private String permissionUser;

    public String getNameUser() {
        return nameUser;
    }

    public void setNameUser(String nameUser) {
        this.nameUser = nameUser;
    }

    public String getPermissionUser() {
        return permissionUser;
    }

    public void setPermissionUser(String permissionUser) {
        this.permissionUser = permissionUser;
    }
}
