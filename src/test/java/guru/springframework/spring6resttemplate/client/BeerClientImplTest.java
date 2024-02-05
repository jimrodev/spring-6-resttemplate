package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BeerClientImplTest {

    @Autowired
    BeerClient beerClient;

    @Test
    void list() {

        beerClient.list(null, null, null, null, null);
    }

    @Test
    void listByName() {

        beerClient.list("ALE", null, null, null, null);
    }

    @Test
    void listOnlyOne() {

        Page<BeerDTO> beers = beerClient.list(null, null, null, null, 1);

        assertThat(beers.getContent().size()).isEqualTo(1);
    }

    @Test
    void getById() {

        Page<BeerDTO> beers = beerClient.list(null, null, null, null,1);

        BeerDTO beer = beers.getContent().get(0);

        BeerDTO beerById = beerClient.getById(beer.getId());

        assertNotNull(beerById);

    }

    @Test
    void create() {

        BeerDTO newBeer = BeerDTO.builder()
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("12345")
                .build();

        BeerDTO savedBeer = beerClient.create(newBeer);

        assertNotNull(savedBeer);
    }

    @Test
    void update() {
        // 1 - Creamos un nuevo Beer
        BeerDTO newBeer = BeerDTO.builder()
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("12345")
                .build();

        // Los guardamos en la bbdd
        BeerDTO savedBeer = beerClient.create(newBeer);

        // Lo actualizamos
        final String newName = "Mango Bobs 3";
        savedBeer.setBeerName(newName);
        BeerDTO updatedBeer = beerClient.update(savedBeer);

        // Validamos que el nombre ha sido actualizado
        assertEquals(newName, updatedBeer.getBeerName());
    }

    @Test
    void deleteBeer() {

        // 1 - Creamos un nuevo Beer
        BeerDTO newBeer = BeerDTO.builder()
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("12345")
                .build();

        // Los guardamos en la bbdd
        BeerDTO savedBeer = beerClient.create(newBeer);

        // Lo eliminamos
        beerClient.delete(savedBeer.getId());

        // Validamos que no existe en la bbdd
        // Si comentamos el assertThows obtenemos el siguiente error -> del tipo HttpClientErrorException
        // ERROR:
        // org.springframework.web.client.HttpClientErrorException$NotFound: 404 : "{"timestamp":"2024-02-01T13:04:08.4511801","status":404,"error":"Not Found","message":null,"path":"/api/v1/beer/4a338d9c-c9fe-4541-b97e-eb94e58be09c"}"

        assertThrows(HttpClientErrorException.class, () -> {
            // should error
            beerClient.getById(savedBeer.getId());
        });
    }
}