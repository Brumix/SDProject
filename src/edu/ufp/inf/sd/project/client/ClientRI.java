package edu.ufp.inf.sd.project.client;

import java.rmi.RemoteException;

public interface ClientRI {
    void printResult(WorkerRI w, Integer result)throws RemoteException;

}
