package com.robopupu.api.mvp;

/*
 * {@link com.robopupu.api.mvp.PresenterListener} a listener interface for receiving lifecycle events
 * from a {@link Presenter}.
 */
public interface PresenterListener {
    /*
     * Invoked by a {@link Presenter} when it is finished by invoking {@link Presenter#finish()}.
     *
     * @param presenter The finished {@link Presenter}.
     */
    void onPresenterFinished(Presenter presenter);
}
