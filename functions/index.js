const {onDocumentWritten} = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
admin.initializeApp();
const regions = ['Вінницька','Волинська','Дніпропетровська','Донецька','Житомирська','Закарпатська','Запорізька','Івано-Франківська','Київська','Кіровоградська','Луганська','Львівська','Миколаївська','Одеська','Полтавська','Рівненська','Сумська','Тернопільська','Харківська','Херсонська','Хмельницька','Черкаська','Чернівецька','Чернігівська','м. Київ'];
exports.alertChanged = onDocumentWritten('alerts/{region}', async event => {
  const before=event.data.before.data()?.type ?? 'none';
  const after=event.data.after.data()?.type ?? 'none';
  if(before===after || !['none','fart','burp'].includes(after)) return;
  const index=regions.indexOf(event.params.region);
  if(index<0)return;
  const message=after==='fart'?'Пердёжная тревога!':after==='burp'?'Відрижна тривога!':'Відбій тривоги';
  await admin.messaging().send({topic:`region_${index}`,notification:{title:`ПукТривога • ${event.params.region}`,body:message},android:{priority:'high',notification:{channelId:'alerts',sound:'default'}},data:{region:event.params.region,type:after}});
});
