/*
 * Created by Darshan Pandya.
 * @itznotabug
 * Copyright (c) 2018.
 */

package io.luda.houseads.modal;

import java.io.Serializable;

public class InterstitialModal implements Serializable {

    public long duration;
    private String interstitialImageUrl;
    private String packageOrUrl;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    private String clientId;
    private String adId;

    public void setInterstitialImageUrl(String interstitialImageUrl) {
        this.interstitialImageUrl = interstitialImageUrl;
    }

    public void setPackageOrUrl(String packageName) {
        this.packageOrUrl = packageName;
    }

    public String getInterstitialImageUrl() {
        return interstitialImageUrl;
    }

    public String getPackageOrUrl() {
        return packageOrUrl;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
