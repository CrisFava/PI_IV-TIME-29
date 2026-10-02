# Como criar um handler.

### Antes de criar um handler, você deve saber algumas coisas: 

- Você deve criar 3 classes, sendo elas: Um request, um response e o próprio handler.
- A classe handler deve ter em sua assinatura: `RequestHandler<T, R>`. Onde T = Request, R = Response.
- Request e Response devem ter em sua assinatura: `extends Message`. e ter o atributo obrigatório abaixo:
- ```java 
    @Serial
    private static final long serialVersionUID = 1L;
- Após o request e o response criados, você pode implementar o método: `public R execute(T request)`

# Por que existe o atributo obrigatório? 
Colocar explicitamente:
```java
private static final long serialVersionUID = 1L;
```

Garante estabilidade e compatibilidade:

- Informa à JVM: "Mesmo que o cliente e o servidor tenham sido compilados em momentos ou máquinas diferentes, essas classes pertencem à mesma versão e são compatíveis".
- Evita avisos de compilação do Java.
- Se no futuro você fizer uma mudança drástica na classe e quiser forçar a incompatibilidade com clientes antigos, basta mudar para 2L.