package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.springframework.data.domain.Page;

import java.util.UUID;

// V192
// Page from org.springframework.data.domain.Page;
// Tras incluir la dependencia
// <dependency>
//    <groupId>org.springframework.data</groupId>
//    <artifactId>spring-data-commons</artifactId>
// </dependency>
public interface BeerClient {

    Page<BeerDTO> list(String name,
                       BeerStyle style,
                       Boolean showInventory,
                       Integer pageNumber,
                       Integer pageSize);

    BeerDTO getById(UUID id);

    BeerDTO create(BeerDTO newBeer);

    BeerDTO update(BeerDTO newBeer);

    void delete(UUID id);
}
