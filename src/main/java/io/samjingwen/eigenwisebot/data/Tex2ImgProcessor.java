package io.samjingwen.eigenwisebot.data;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
public class Tex2ImgProcessor {

  public void generateImages() {
    String projectRoot = System.getProperty("user.dir");
    String executablePath = projectRoot + "/util/tex2img";

    for (int i = 1; i <= 5; i++) {
      String inputTex =
          String.format("%s/src/main/resources/quiz/%s/raw.tex", projectRoot, i);
      String outputImg =
          String.format("%s/src/main/resources/quiz/%s/img.jpg", projectRoot, i);

      // Check if input file exists before attempting conversion
      if (!new File(inputTex).exists()) {
        log.warn("Skipping {}: raw.tex not found.", i);
        continue;
      }

      ProcessBuilder pb =
          new ProcessBuilder(
              executablePath, "--margins", "30", "--unit", "px", inputTex, outputImg);

      try {
        log.info("Processing {}...", i);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
          log.info("Successfully generated image for {}", i);
        } else {
          log.error("Failed to generate image for {}. Exit code: {}", i, exitCode);
        }
      } catch (IOException | InterruptedException e) {
        log.error("Error executing tex2img for {}", i, e);
        Thread.currentThread().interrupt();
      }
    }
  }

  public static void main(String[] args){
    new Tex2ImgProcessor().generateImages();
  }
}
