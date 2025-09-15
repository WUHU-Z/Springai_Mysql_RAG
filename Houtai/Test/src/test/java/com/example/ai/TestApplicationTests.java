package com.example.ai;

import com.example.ai.utils.VectorDistanceUtils;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;

@SpringBootTest

class TestApplicationTests {

//    @Autowired
//    private OpenAiEmbeddingModel embeddingModel;

    @Autowired
    private VectorStore myVectorStore;

    @Test
    void testVectorStore() {
        Resource resource = new FileSystemResource("mytest.pdf");
        System.out.println("文件是否存在: " + resource.exists());
        //1.创建PDF的读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1)
                        .build()
        );
        //2.读取PDF,拆分为Document
        List<Document>documents = reader.read();
        System.out.println("解析出的文档数量: " + documents.size());
        //3.写入向量库
        myVectorStore.add(documents);
        //4.搜索
        SearchRequest request = SearchRequest.builder()
                .query("属性分析")
                .topK(1)
//                .similarityThreshold(0.7)
                .build();

        List<Document> docs = myVectorStore.similaritySearch(request);

        if(docs == null || docs.isEmpty()){
            System.out.println("没有搜索到任何内容");
        }
        for(Document doc : docs){
            System.out.println(doc.getId());
            System.out.println(doc.getScore());
            System.out.println(doc.getText());
        }
    }

//    @Test
//    void contextLoads() {
//        String query = "国际冲突";
//
//        // 1.2.用来做比较的文本
//        String[] texts = new String[]{
//                "哈马斯加沙阶段停火谈判仍在进行, 以方尚未做出承诺",
//                "土耳其、芬兰、瑞典与北约代表将继续就瑞典“入约”问题进行谈判",
//                "日本航空基地水井中检测出有机氟化物超标",
//                "国家游泳中心（水立方）: 恢复游泳、嬉水乐园等水上项目运营",
//                "我国首次在空间站开展舱外辐射生物学暴露实验"
//        };
//
//        // 2.向量化
//        // 2.1.先查询文本向量化
//        float[] queryVector = embeddingModel.embed(query);
//
//        // 2.2.再将比较文本向量化，放到一个数组
//        List<float[]> textVectors = embeddingModel.embed(Arrays.asList(texts));
//
//        // 3.比较欧氏距离
//        // 3.1.把查询文本自己与自己比较，肯定是相似度最高的
//        System.out.println(VectorDistanceUtils.euclideanDistance(queryVector, queryVector));
//        final var length = queryVector.length;
//        System.out.println(length);
//
//        // 计算texts中每个文本与查询文本的余弦距离和欧氏距离
//        System.out.println("--- 欧氏距离 (Euclidean Distance) ---");
//        for (int i = 0; i < texts.length; i++) {
//            double distance = VectorDistanceUtils.euclideanDistance(queryVector, textVectors.get(i));
//            System.out.printf("文本: \"%s\"\n欧氏距离: %.4f\n\n", texts[i], distance);
//        }
//
//        System.out.println("--- 余弦距离 (Cosine Distance) ---");
//        for (int i = 0; i < texts.length; i++) {
//            double distance = VectorDistanceUtils.cosineDistance(queryVector, textVectors.get(i));
//            System.out.printf("文本: \"%s\"\n余弦距离: %.4f\n\n", texts[i], distance);
//        }
//
//    }
}
