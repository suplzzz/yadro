package com.suplz.yadro;

interface IDeDuplicationCallback {
    void onSuccess();

    void onError(String message);

    void onNoDuplicatesFound();
}