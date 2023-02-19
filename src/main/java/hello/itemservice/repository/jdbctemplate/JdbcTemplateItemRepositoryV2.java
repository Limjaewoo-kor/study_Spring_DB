package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC Template
 * NamedParameterJdbcTemplate
 * * 파라미터의 순서가 바뀔 수 있다.
 * SqlParameterSource
 * - BeanPropertySqlParameterSource
 * - MapSqlParameterSource
 * Map
 *
 * BeanPropertyRowMapper
 */
@Slf4j
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

//    private final JdbcTemplate template;

    private final NamedParameterJdbcTemplate template;

//    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
//        this.template = new JdbcTemplate(dataSource);
//    }
    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Item save(Item item) {
        String sql = "insert into item(item_name, price, quantity) " +
                "values (:itemName,:price,:quantity)";

        //넘긴 객체 이름으로 파라미터를 만들어줌
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        template.update(sql,param,keyHolder);

//        template.update(connection -> {
//            //자동 증가 키
//            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
//            ps.setString(1, item.getItemName());
//            ps.setInt(2, item.getPrice());
//            ps.setInt(3, item.getQuantity());
//            return ps;
//        }, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);

        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=:itemName, price=:price, quantity=:quantity " +
                "where id=:id";


        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);

        template.update(sql,param);

//        template.update(sql,
//                        updateParam.getItemName(),
//                        updateParam.getPrice(),
//                        updateParam.getQuantity(),
//                        itemId);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = :id";

        try{
            Map<String,Object> param = Map.of("id",id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        }catch (EmptyResultDataAccessException e){
            return Optional.empty();
        }
//        try{
//            Item item = template.queryForObject(sql, itemRowMapper(), id);
//            return Optional.of(item);
//        }catch (EmptyResultDataAccessException e){
//            return Optional.empty();
//        }
    }



    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name as itemName, price, quantity from item";

        //동적 쿼리
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;

//        List<Object> param = new ArrayList<>();

        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
//            param.add(itemName);
            andFlag = true;
        }

        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
//            param.add(maxPrice);
        }

        log.info("sql={}", sql);

//        return template.query(sql,itemRowMapper(),param.toArray());
        return template.query(sql, param, itemRowMapper());
    }


    private RowMapper<Item> itemRowMapper() {
//        return ((rs, rowNum) -> {
//            Item item = new Item();
//            item.setId(rs.getLong("id"));
//            item.setItemName(rs.getString("item_name"));
//            item.setPrice(rs.getInt("price"));
//            item.setQuantity(rs.getInt("quantity"));
//            return item;
//        });
        return BeanPropertyRowMapper.newInstance(Item.class); //camel 변환 지원
    }
}
