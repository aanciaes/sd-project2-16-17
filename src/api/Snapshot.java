/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import java.io.Serializable;

/**
 *
 * @author miguel
 */
public class Snapshot implements Serializable{
    
    private String test;

    public Snapshot(String test) {
        this.test = test;
    }

    public String getTest() {
        return test;
    }   
}
