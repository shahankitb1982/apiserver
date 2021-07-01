package com.apiserver.services;

import com.apiserver.model.Product;
import com.apiserver.repository.ProductRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;

@Service
public class ProductServices {

    private final ProductRepository productRepository;

    public ProductServices(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Sync shopify data to database.
     *
     * @throws Exception
     */
    public void syncShopifyData() throws Exception {
        String allData = "";
        try {
            allData = getShopifyApiData();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        if (StringUtils.isNotEmpty(allData)) {
            JSONObject productObject = new JSONObject(allData);
            if (productObject.has("products")) {
                JSONArray productsArray = productObject.getJSONArray("products");
                System.out.println("Total products : " + productsArray.length());
                if (productsArray.length() > 0) {
                    for (int i = 0; i < productsArray.length(); i++) {
                        JSONObject products = productsArray.getJSONObject(i);
                        Product product = new Product();
                        if (products.has("id")) {
                            product.setId(products.getLong("id"));
                        }

                        if (products.has("title")) {
                            product.setTitle(products.getString("title"));
                        }

                        int totalQuantity = 0;
                        if (products.has("variants")) {
                            JSONArray productVariantsArray = products.getJSONArray("variants");
                            for (int j = 0; j < productVariantsArray.length(); j++) {
                                JSONObject productVariants = productVariantsArray.getJSONObject(j);
                                if (productVariants.has("inventory_quantity")) {
                                    totalQuantity = totalQuantity + productVariants.getInt("inventory_quantity");
                                }
                            }
                        }

                        String allColors = "", allSizes = "";
                        if (products.has("options")) {
                            JSONArray optionsArray = products.getJSONArray("options");
                            for (int k = 0; k < optionsArray.length(); k++) {
                                JSONObject optionsObj = optionsArray.getJSONObject(k);
                                if (optionsObj.has("name")) {
                                    if (optionsObj.getString("name").equals("Size")) {
                                        allSizes = StringUtils.join(optionsObj.getJSONArray("values"), ',');
                                    }

                                    if (optionsObj.getString("name").equals("Color")) {
                                        allColors = StringUtils.join(optionsObj.getJSONArray("values"), ',');
                                    }
                                }
                            }
                        }
                        product.setColor(allColors);
                        product.setSize(allSizes);
                        product.setQuantity(totalQuantity);
                        product = productRepository.save(product);
                        System.out.println("Product created : " + product.getId());
                    }
                }
            }
        } else {
            throw new Exception("No data found.");
        }
    }

    /**
     * To get data from Shopify Product API
     *
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    public String getShopifyApiData() throws URISyntaxException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = "https://take-home-test-store.myshopify.com/admin/api/2021-04/products.json";

        URIBuilder builder = new URIBuilder(url);

        HttpGet request = new HttpGet(builder.build());
        request.setHeader("X-Shopify-Access-Token", "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");
        request.setHeader("Content-Type", "application/json");

        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        String responseResult = EntityUtils.toString(entity);

        return responseResult;
    }
}
