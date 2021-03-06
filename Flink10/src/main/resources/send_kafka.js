// vi kafka.js
var kafka = require('kafka-node');
var Mock = require('mockjs');
const Random = Mock.Random;

let conn = { 'kafkaHost': 'skuldcdhtest1.ktcs.com:9092' };
var MQ = function () {
    this.mq_producers = {};
    this.client = {};
}


MQ.prototype.AddProducer = function (conn, handler) {
    console.log('增加生产者', conn, this);
    this.client = new kafka.KafkaClient(conn);
    let producer = new kafka.Producer(this.client);

    producer.on('ready', function () {
        if (!!handler) {
            handler(producer);
        }
    });

    producer.on('error', function (err) {
        console.error('producer error ', err.stack);
    });

    this.mq_producers['common'] = producer;
    return producer;
}

console.log(MQ);
var mq = new MQ();


//topic 名称写入时候，会先创建topic，如果不存在的话
var topicName = "test01"
var datajson =  {
    // "business": "sdasf",
    // "database": "sqweqr",
    // "es": 2314,
    // "sql": "",
    // "table": "t_cash_loan",
    // "ts": 1576050001925,
    // "type": "UPDATE",
	"param_data": {
		"platform_s": "ADR",
		"category_s": "event_role",
		"dt": "2020-04-26"
	}
}

mq.AddProducer(conn, function (producer) {
    producer.createTopics([topicName], function () {
        setInterval(function () {
            //只需要改这开就可以了，了解mockjs的数据用法
            let data = Mock.mock(datajson)
            let msg = JSON.stringify(data)

            var _msg = {
                topic: [topicName],
                messages: msg
            }
            // console.log('clientId : ',mq.client.clientId);
            // console.log('topicMetadata ',mq.client.topicMetadata);
            // console.log('brokerMetadata ',mq.client.brokerMetadata);
            // console.log('clusterMetadata ',mq.client.clusterMetadata);
            // console.log('brokerMetadataLastUpdate ',mq.client.brokerMetadataLastUpdate);
            mq.mq_producers['common'].send([_msg], function (err, data) {
                console.log("send you can check \n kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic "+topicName+" --from-beginning \n", data);
            })
        }, 2000);
    })
});