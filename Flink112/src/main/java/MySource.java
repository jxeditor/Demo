import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.UUID;

public class MySource implements SourceFunction<Tuple4<String,Long,Integer,Integer>> {

		static int status[] = {200, 404, 500, 501, 301};

		@Override
		public void run(SourceContext<Tuple4<String,Long,Integer,Integer>> sourceContext) throws Exception{
			while (true){
				Thread.sleep((int) (Math.random() * 100));
				// traceid,timestamp,status,response time

				Tuple4 log = Tuple4.of(
						UUID.randomUUID().toString(),
						System.currentTimeMillis(),
						status[(int) (Math.random() * 4)],
						(int) (Math.random() * 100));

				sourceContext.collect(log);
			}
		}

		@Override
		public void cancel(){

		}
	}