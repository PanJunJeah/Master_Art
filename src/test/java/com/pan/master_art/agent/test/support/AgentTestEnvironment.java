package com.pan.master_art.agent.test.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pan.master_art.agent.AgentPrompts;
import com.pan.master_art.agent.config.AgentConfig;
import com.pan.master_art.agent.orchestration.CuratorAgentService;
import com.pan.master_art.agent.rag.KnowledgeRetriever;
import com.pan.master_art.agent.tool.AgentArtworkTools;
import com.pan.master_art.agent.tool.AgentExhibitionTools;
import com.pan.master_art.agent.tool.AgentReservationTools;
import com.pan.master_art.agent.tool.AgentSubscriptionTools;
import com.pan.master_art.entity.Artwork;
import com.pan.master_art.entity.Artist;
import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.entity.ReservationDraft;
import com.pan.master_art.entity.TicketSession;
import com.pan.master_art.repository.ArtworkRepository;
import com.pan.master_art.repository.ExhibitionRepository;
import com.pan.master_art.repository.ExhibitionSubscriptionRepository;
import com.pan.master_art.repository.ReservationDraftRepository;
import com.pan.master_art.repository.ReservationRepository;
import com.pan.master_art.repository.TicketSessionRepository;
import com.pan.master_art.service.ReservationDraftService;
import com.pan.master_art.service.ReservationService;
import com.pan.master_art.service.SubscriptionService;
import com.pan.master_art.service.TicketSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Agent 编排层单元测试环境（与生产同构）：
 * - 模型层：真实 OpenAiChatModel（Spring AI 原生内部工具调用循环），
 *   仅把 HTTP 端点替换为脚本化本地服务 ScriptedOpenAiServer（模拟 DeepSeek 的 OpenAI 兼容协议）；
 * - 工具层：4 个真实 @Tool 工具 + AgentConfig.buildGuardedToolCallbacks 步数守卫（与生产同一条装配路径）；
 * - 数据层：Mockito 模拟仓库，服务层全部真实实现。
 */
public class AgentTestEnvironment implements AutoCloseable {

    public final ArtworkRepository artworkRepository = mock(ArtworkRepository.class);
    public final ExhibitionRepository exhibitionRepository = mock(ExhibitionRepository.class);
    public final ReservationDraftRepository draftRepository = mock(ReservationDraftRepository.class);
    public final TicketSessionRepository sessionRepository = mock(TicketSessionRepository.class);
    public final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    public final ExhibitionSubscriptionRepository subscriptionRepository = mock(ExhibitionSubscriptionRepository.class);

    public final ObjectMapper objectMapper = new ObjectMapper();
    public final ReservationDraftService draftService =
            new ReservationDraftService(draftRepository, sessionRepository, exhibitionRepository, 30);
    public final ReservationService reservationService =
            new ReservationService(reservationRepository, draftRepository, sessionRepository);
    public final TicketSessionService sessionService = new TicketSessionService(sessionRepository);
    public final SubscriptionService subscriptionService =
            new SubscriptionService(subscriptionRepository, exhibitionRepository);

    public final List<ReservationDraft> savedDrafts = new ArrayList<>();
    public final ScriptedOpenAiServer server;
    public final MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
    public final CuratorAgentService agentService;
    public final int maxSteps;

    public static final long USER_ID = 42L;

    public AgentTestEnvironment(ScriptedOpenAiServer server, KnowledgeRetriever retriever, int maxSteps) {
        this.server = server;
        this.maxSteps = maxSteps;

        stubSaveDraft();
        stubDefaultRepos();

        AgentArtworkTools artworkTools = new AgentArtworkTools(artworkRepository, retriever, objectMapper);
        AgentExhibitionTools exhibitionTools = new AgentExhibitionTools(exhibitionRepository, sessionService, objectMapper);
        AgentReservationTools reservationTools = new AgentReservationTools(draftService, objectMapper);
        AgentSubscriptionTools subscriptionTools = new AgentSubscriptionTools(subscriptionService);

        ToolCallback[] callbacks = AgentConfig.buildGuardedToolCallbacks(maxSteps,
                artworkTools, exhibitionTools, reservationTools, subscriptionTools);

        // 真实 OpenAiChatModel：内部工具调用循环完全由 Spring AI 原生机制驱动
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(server.baseUrl())
                .apiKey("test-key-not-used")
                .completionsPath("/v1/chat/completions")
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.2)
                        .internalToolExecutionEnabled(true)
                        .build())
                .toolCallingManager(DefaultToolCallingManager.builder().build())
                .build();

        ChatClient chatClient = ChatClient.builder(model)
                .defaultSystem(AgentPrompts.SYSTEM_PROMPT)
                .defaultToolCallbacks(callbacks)
                .build();

        this.agentService = new CuratorAgentService(chatClient, chatMemory, draftService, objectMapper);
    }

    // ---------------- 夹具数据 ----------------

    public Exhibition exhibition() {
        Exhibition e = new Exhibition();
        e.setId(1L);
        e.setName("印象派大师展");
        e.setTheme("印象派");
        e.setLocation("上海·西岸美术馆");
        e.setStartTime(new Date(System.currentTimeMillis() - 86400_000L));
        e.setEndTime(new Date(System.currentTimeMillis() + 7 * 86400_000L));
        e.setStatus(Exhibition.ExhibitionStatus.ONGOING);
        e.setVisitInfo("开放时间 10:00-18:00，凭票入场");
        return e;
    }

    public TicketSession session() {
        TicketSession s = new TicketSession();
        s.setId(11L);
        s.setExhibition(exhibition());
        s.setSessionTime(new Date(System.currentTimeMillis() + 2 * 86400_000L));
        s.setTotalTickets(100);
        s.setSoldTickets(30);
        s.setPrice(new BigDecimal("88.00"));
        s.setStatus(1);
        return s;
    }

    public Artwork artwork(long id, String name, String genre) {
        Artist artist = new Artist();
        artist.setId(7L);
        artist.setName("文森特·梵高");
        Artwork a = new Artwork();
        a.setId(id);
        a.setName(name);
        a.setEnglishName("The Starry Night");
        a.setGenre(genre);
        a.setCreationYear("1889");
        a.setType(1);
        a.setArtist(artist);
        a.setStatus(1);
        a.setDescription("后印象派代表作，漩涡状的星空与柏树。");
        return a;
    }

    // ---------------- Mock 桩 ----------------

    private void stubSaveDraft() {
        when(draftRepository.saveAndFlush(any(ReservationDraft.class))).thenAnswer(inv -> {
            ReservationDraft draft = inv.getArgument(0);
            if (draft.getId() == null) {
                draft.setId(100L);
            }
            savedDrafts.add(draft);
            return draft;
        });
        when(draftRepository.save(any(ReservationDraft.class))).thenAnswer(inv -> inv.getArgument(0));
        when(draftRepository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return savedDrafts.stream().filter(d -> id.equals(d.getId())).map(Optional::of).findFirst()
                    .orElse(Optional.empty());
        });
        when(draftRepository.findByUserIdAndIdempotencyKey(anyLong(), any())).thenAnswer(inv -> {
            Long uid = inv.getArgument(0);
            String key = inv.getArgument(1);
            return savedDrafts.stream()
                    .filter(d -> uid.equals(d.getUserId()) && key.equals(d.getIdempotencyKey()))
                    .findFirst();
        });
    }

    private void stubDefaultRepos() {
        when(exhibitionRepository.findById(1L)).thenAnswer(inv -> Optional.of(exhibition()));
        when(sessionRepository.findById(11L)).thenAnswer(inv -> Optional.of(session()));
        when(artworkRepository.searchForAgent(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(5);
                    List<Artwork> list = List.of(artwork(1L, "星空", "后印象派"), artwork(2L, "睡莲", "印象派"));
                    return new PageImpl<>(list, pageable, 2);
                });
        when(artworkRepository.findByStatusOrderByViewCountDesc(anyInt(), any(PageRequest.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(artwork(1L, "星空", "后印象派")), inv.getArgument(1), 1));
        when(sessionRepository.findByExhibitionIdAndStatus(anyLong(), anyInt(), any()))
                .thenAnswer(inv -> List.of(session()));
        when(sessionRepository.findByExhibitionId(anyLong(), any()))
                .thenAnswer(inv -> List.of(session()));
        when(exhibitionRepository.findByNameContainingOrEnglishNameContaining(any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(exhibition()), inv.getArgument(2), 1));
        when(exhibitionRepository.findCurrentExhibitions(any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(exhibition()), inv.getArgument(2), 1));
        when(exhibitionRepository.findUpcomingExhibitions(any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(2), 0));
    }

    /** 指定 findById 返回的对象 */
    public void stubSessionAndExhibition(TicketSession session, Exhibition exhibition) {
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(exhibitionRepository.findById(exhibition.getId())).thenReturn(Optional.of(exhibition));
    }

    @Override
    public void close() {
        server.close();
    }
}
