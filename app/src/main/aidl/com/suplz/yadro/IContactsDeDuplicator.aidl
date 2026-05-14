package com.suplz.yadro;

import com.suplz.yadro.IDeDuplicationCallback;
import com.suplz.yadro.data.model.AidlContact;

interface IContactsDeDuplicator {
    List<AidlContact> getContacts();
    void removeDuplicates(IDeDuplicationCallback callback);
}