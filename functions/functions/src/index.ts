import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();
const db = admin.firestore();

export const loadads = functions.https.onRequest((request, response) => {

    db.collection('ads').where('status','==','active').onSnapshot(value => {
        const ads: {
            id: string;
            mediaUrl?: string;
            clientId?: string;
            duration?: number;
        }[] = value.docs.map(doc => { 
            const id = doc.id;
            return {... doc.data(), id} 
        })
        console.log(ads)
        const ad = ads[Math.floor(Math.random() * ads.length)];
        response.send(`{
              "apps":[{
                 "app_interstitial_url": "${ad.mediaUrl}",
                 "client_id":"${ad.clientId}",
                 "ad_id":"${ad.id}",
                 "duration":${ad.duration},
                 "app_uri": "",
                 "app_adType": "interstitial"
              }]
          }`)
    })
    return 0;

});


